package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.common.security.Rol;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.importacion.FilaErrorDto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.usuarios.Usuario;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoOrganizacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.UsuarioRepository;

@Component
public class ImportadorDeDonantes {

    private final DonanteRepository donanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final RestDonacionesConfig config;

    private final AtomicInteger contador = new AtomicInteger(100);
    private final Map<String, ImportacionTarea> tareasPorId = new ConcurrentHashMap<>();
    private final List<ImportacionTarea> tareas = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    public ImportadorDeDonantes(DonanteRepository donanteRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            RestTemplate restTemplate,
            RestDonacionesConfig config) {
        this.donanteRepository = donanteRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
        this.config = config;
    }

    public ImportadorDeDonantes(DonanteRepository donanteRepository, RestTemplate restTemplate,
            RestDonacionesConfig config) {
        this(donanteRepository, null, null, restTemplate, config);
    }

    private void registrarTarea(ImportacionTarea tarea) {
        tareasPorId.put(tarea.getId(), tarea);
        tareas.add(0, tarea);
    }

    public List<ImportacionTarea> obtenerImportaciones() {
        return Collections.unmodifiableList(tareas);
    }

    public Optional<ImportacionTarea> obtenerImportacion(String id) {
        return Optional.ofNullable(tareasPorId.get(id));
    }

    /**
     * Inicia una importación en segundo plano desde un stream CSV, sin bloquear el hilo solicitante.
     */
    public ImportacionTarea iniciarImportacion(InputStream inputStream, String nombreArchivo) {
        if (inputStream == null) {
            throw new IllegalArgumentException("El contenido del archivo no puede ser nulo");
        }
        String id = "IMP-" + contador.incrementAndGet();
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        Path tempFile;
        int totalFilas = 0;
        try {
            tempFile = Files.createTempFile("importacion-", ".csv");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            try (BufferedReader br = Files.newBufferedReader(tempFile)) {
                String linea;
                boolean esCabecera = true;
                while ((linea = br.readLine()) != null) {
                    if (linea.trim().isEmpty()) {
                        continue;
                    }
                    if (esCabecera) {
                        esCabecera = false;
                        continue;
                    }
                    totalFilas++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al procesar el archivo CSV temporal: " + e.getMessage(), e);
        }

        ImportacionTarea tarea = new ImportacionTarea(
                id,
                nombreArchivo != null && !nombreArchivo.isBlank() ? nombreArchivo : "donantes.csv",
                fecha,
                "EN_PROCESO",
                totalFilas
        );
        registrarTarea(tarea);

        executor.submit(() -> procesarArchivoAsync(tarea, tempFile));
        return tarea;
    }

    private void procesarArchivoAsync(ImportacionTarea tarea, Path tempFile) {
        try (BufferedReader br = Files.newBufferedReader(tempFile)) {
            String linea;
            boolean esCabecera = true;
            int numeroFila = 0;
            while ((linea = br.readLine()) != null) {
                numeroFila++;
                if (linea.trim().isEmpty()) {
                    continue;
                }
                if (esCabecera) {
                    esCabecera = false;
                    continue;
                }
                procesarFilaBatch(tarea, numeroFila, linea);
            }
            tarea.setEstado("COMPLETADA");
        } catch (Exception e) {
            tarea.setEstado("FALLIDA");
            tarea.agregarError(new FilaErrorDto(0, "-", "Error inesperado durante la importación: " + e.getMessage()));
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
        }
    }

    private void procesarFilaBatch(ImportacionTarea tarea, int numeroFila, String linea) {
        tarea.incrementarProcesadas();
        String[] campos = linea.split(",");
        if (campos.length < 5) {
            tarea.incrementarDescartadas();
            tarea.agregarError(new FilaErrorDto(numeroFila, linea, "Columnas insuficientes (mínimo 5)"));
            return;
        }

        String tipoPersona = campos[0].trim();
        String documento = campos[2].trim();
        String nombreORazonSocial = campos[3].trim();
        String emailVal = campos[4].trim();
        String telefonoVal = campos.length > 5 ? campos[5].trim() : "";

        if (emailVal.isEmpty()) {
            tarea.incrementarDescartadas();
            tarea.agregarError(new FilaErrorDto(numeroFila, nombreORazonSocial.isEmpty() ? "Fila " + numeroFila : nombreORazonSocial, "Email obligatorio vacío"));
            return;
        }
        if (documento.isEmpty()) {
            tarea.incrementarDescartadas();
            tarea.agregarError(new FilaErrorDto(numeroFila, emailVal, "Documento obligatorio vacío"));
            return;
        }

        try {
            if (tipoPersona.equalsIgnoreCase("HUMANA")) {
                PersonaHumana humana = procesarHumana(documento, nombreORazonSocial, emailVal, telefonoVal);
                boolean creada = guardarHumanaBatch(humana, emailVal, telefonoVal);
                if (creada) {
                    tarea.incrementarCreados();
                } else {
                    tarea.incrementarActualizados();
                }
            } else if (tipoPersona.equalsIgnoreCase("JURIDICA")) {
                PersonaJuridica juridica = procesarJuridica(documento, nombreORazonSocial, emailVal, telefonoVal);
                boolean creada = guardarJuridicaBatch(juridica, emailVal, telefonoVal);
                if (creada) {
                    tarea.incrementarCreados();
                } else {
                    tarea.incrementarActualizados();
                }
            } else {
                tarea.incrementarDescartadas();
                tarea.agregarError(new FilaErrorDto(numeroFila, tipoPersona, "Tipo de persona inválido ('" + tipoPersona + "'). Debe ser HUMANA o JURIDICA"));
            }
        } catch (BusinessException e) {
            tarea.incrementarDescartadas();
            tarea.agregarError(new FilaErrorDto(numeroFila, emailVal, e.getMessage()));
        } catch (Exception e) {
            tarea.incrementarDescartadas();
            tarea.agregarError(new FilaErrorDto(numeroFila, emailVal, "Error: " + e.getMessage()));
        }
    }

    private boolean guardarHumanaBatch(PersonaHumana humanaNueva, String emailVal, String telefonoVal) {
        Optional<PersonaHumana> existenteOpt = donanteRepository.buscarPorEmail(emailVal)
                .map(d -> (PersonaHumana) d);

        if (existenteOpt.isPresent()) {
            PersonaHumana humana = existenteOpt.get();
            humana.setNombre(humanaNueva.getNombre());
            humana.setApellido(humanaNueva.getApellido());
            humana.setDni(humanaNueva.getDni());
            actualizarContactos(humana.getContactos(), emailVal, telefonoVal);
            donanteRepository.save(humana);
            crearUsuarioSiNoExiste(emailVal, humana.getId());
            return false;
        } else {
            donanteRepository.save(humanaNueva);
            crearUsuarioSiNoExiste(emailVal, humanaNueva.getId());
            String nombreAMostrar = humanaNueva.getNombre()
                    + (humanaNueva.getApellido().isEmpty() ? "" : " " + humanaNueva.getApellido());
            notificarAltaAsync(emailVal, nombreAMostrar, humanaNueva.getId());
            return true;
        }
    }

    private boolean guardarJuridicaBatch(PersonaJuridica juridicaNueva, String emailVal, String telefonoVal) {
        Optional<PersonaJuridica> existenteOpt = donanteRepository.buscarPorEmail(emailVal)
                .map(d -> (PersonaJuridica) d);

        if (existenteOpt.isPresent()) {
            PersonaJuridica juridica = existenteOpt.get();
            juridica.setRazonSocial(juridicaNueva.getRazonSocial());
            juridica.setCuit(juridicaNueva.getCuit());
            actualizarContactos(juridica.getContactos(), emailVal, telefonoVal);
            donanteRepository.save(juridica);
            crearUsuarioSiNoExiste(emailVal, juridica.getId());
            return false;
        } else {
            donanteRepository.save(juridicaNueva);
            crearUsuarioSiNoExiste(emailVal, juridicaNueva.getId());
            notificarAltaAsync(emailVal, juridicaNueva.getRazonSocial(), juridicaNueva.getId());
            return true;
        }
    }

    private void crearUsuarioSiNoExiste(String email, Long donanteId) {
        if (usuarioRepository == null || passwordEncoder == null || email == null || donanteId == null) {
            return;
        }
        try {
            if (usuarioRepository.buscarPorUsername(email).isEmpty()) {
                String passTemp = "DonaTrack" + (int)(Math.random() * 9000 + 1000);
                String hash = passwordEncoder.encode(passTemp);
                Usuario usuario = new Usuario(email, hash, Rol.DONANTE, donanteId);
                usuarioRepository.save(usuario);
            }
        } catch (Exception e) {
            System.err.println("No se pudo crear usuario para el donante importado: " + e.getMessage());
        }
    }

    private void notificarAltaAsync(String emailVal, String nombre, Long donanteId) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = config.getNotificacionesUrl() + "/notificaciones";
                NotificacionRequest request = new NotificacionRequest(
                        emailVal,
                        TipoContacto.EMAIL,
                        "¡Bienvenido a DonaTrack, " + nombre + "! Tu cuenta ha sido registrada con éxito.",
                        "ALTA_IMPORTACION",
                        "DONANTE",
                        donanteId,
                        null);
                restTemplate.postForObject(url, request, Void.class);
            } catch (Exception ignored) {}
        });
    }

    // ---- Métodos sincrónicos originales para compatibilidad hacia atrás ----

    public void importarDonantes(String pathArchivo) {
        List<Donante> donantes = importarCsv(pathArchivo);
        for (Donante donante : donantes) {
            if (donante instanceof PersonaHumana) {
                procesarHumana((PersonaHumana) donante);
            } else if (donante instanceof PersonaJuridica) {
                procesarJuridica((PersonaJuridica) donante);
            }
        }
    }

    private List<Donante> importarCsv(String pathArchivo) {
        if (pathArchivo == null || pathArchivo.isEmpty()) {
            throw new IllegalArgumentException("La ruta del archivo no puede estar vacía");
        }

        List<Donante> donantes = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(pathArchivo))) {
            String linea;
            boolean esCabecera = true;

            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) {
                    continue;
                }

                if (esCabecera) {
                    esCabecera = false;
                    continue;
                }

                Donante donante = procesarLinea(linea);
                if (donante != null) {
                    donantes.add(donante);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo CSV: " + e.getMessage(), e);
        }

        return donantes;
    }

    private Donante procesarLinea(String linea) {
        String[] campos = linea.split(",");
        if (campos.length < 5) {
            return null;
        }

        String tipoPersona = campos[0].trim();
        String documento = campos[2].trim();
        String nombreORazonSocial = campos[3].trim();
        String emailVal = campos[4].trim();
        String telefonoVal = campos.length > 5 ? campos[5].trim() : "";

        try {
            if (tipoPersona.equalsIgnoreCase("HUMANA")) {
                return procesarHumana(documento, nombreORazonSocial, emailVal, telefonoVal);
            } else if (tipoPersona.equalsIgnoreCase("JURIDICA")) {
                return procesarJuridica(documento, nombreORazonSocial, emailVal, telefonoVal);
            }
        } catch (BusinessException e) {
            System.err.println("Fila de donante descartada (" + e.getMessage() + "): " + linea);
        }

        return null;
    }

    private PersonaHumana procesarHumana(String dni, String nombreCompleto, String emailVal, String telefonoVal) {
        String nombre = nombreCompleto.trim();
        String apellido = "";
        int primerEspacio = nombreCompleto.indexOf(' ');
        if (primerEspacio > 0) {
            nombre = nombreCompleto.substring(0, primerEspacio).trim();
            apellido = nombreCompleto.substring(primerEspacio).trim();
        }

        MedioContacto email = new MedioContacto(emailVal, TipoContacto.EMAIL);
        return new PersonaHumana(armarContactos(email, telefonoVal), email, nombre, apellido,
                null, dni, null, null);
    }

    private PersonaJuridica procesarJuridica(String documento, String razonSocial, String emailVal,
            String telefonoVal) {
        MedioContacto email = new MedioContacto(emailVal, TipoContacto.EMAIL);
        return new PersonaJuridica(armarContactos(email, telefonoVal), email, razonSocial, null,
                TipoOrganizacion.EMPRESA, documento, new ArrayList<>());
    }

    private List<MedioContacto> armarContactos(MedioContacto email, String telefonoVal) {
        List<MedioContacto> contactos = new ArrayList<>();
        contactos.add(email);

        if (!telefonoVal.isEmpty()) {
            MedioContacto tel = new MedioContacto(telefonoVal, TipoContacto.SMS);
            contactos.add(tel);
        }
        return contactos;
    }

    private void procesarHumana(PersonaHumana humanaNueva) {
        String emailVal = obtenerEmailVal(humanaNueva.getContactos());
        if (emailVal == null)
            return;

        Optional<PersonaHumana> existenteOpt = donanteRepository.buscarPorEmail(emailVal)
                .map(d -> (PersonaHumana) d);

        if (existenteOpt.isPresent()) {
            PersonaHumana humana = existenteOpt.get();
            humana.setNombre(humanaNueva.getNombre());
            humana.setApellido(humanaNueva.getApellido());
            humana.setDni(humanaNueva.getDni());
            actualizarContactos(humana.getContactos(), emailVal, obtenerTelefonoVal(humanaNueva.getContactos()));
            donanteRepository.save(humana);
            crearUsuarioSiNoExiste(emailVal, humana.getId());
        } else {
            donanteRepository.save(humanaNueva);
            crearUsuarioSiNoExiste(emailVal, humanaNueva.getId());
            String nombreAMostrar = humanaNueva.getNombre()
                    + (humanaNueva.getApellido().isEmpty() ? "" : " " + humanaNueva.getApellido());
            try {
                String url = config.getNotificacionesUrl() + "/notificaciones";
                NotificacionRequest request = new NotificacionRequest(
                        emailVal,
                        TipoContacto.EMAIL,
                        "¡Bienvenido a DonaTrack, " + nombreAMostrar + "! Tu cuenta ha sido registrada con éxito.",
                        "ALTA_IMPORTACION",
                        "DONANTE",
                        humanaNueva.getId(),
                        null);
                restTemplate.postForObject(url, request, Void.class);
            } catch (Exception e) {
                System.err.println("Error notificando alta de donante humano: " + e.getMessage());
            }
        }
    }

    private void procesarJuridica(PersonaJuridica juridicaNueva) {
        String emailVal = obtenerEmailVal(juridicaNueva.getContactos());
        if (emailVal == null)
            return;

        Optional<PersonaJuridica> existenteOpt = donanteRepository.buscarPorEmail(emailVal)
                .map(d -> (PersonaJuridica) d);

        if (existenteOpt.isPresent()) {
            PersonaJuridica juridica = existenteOpt.get();
            juridica.setRazonSocial(juridicaNueva.getRazonSocial());
            juridica.setCuit(juridicaNueva.getCuit());
            actualizarContactos(juridica.getContactos(), emailVal, obtenerTelefonoVal(juridicaNueva.getContactos()));
            donanteRepository.save(juridica);
            crearUsuarioSiNoExiste(emailVal, juridica.getId());
        } else {
            donanteRepository.save(juridicaNueva);
            crearUsuarioSiNoExiste(emailVal, juridicaNueva.getId());
            try {
                String url = config.getNotificacionesUrl() + "/notificaciones";
                NotificacionRequest request = new NotificacionRequest(
                        emailVal,
                        TipoContacto.EMAIL,
                        "¡Bienvenido a DonaTrack, " + juridicaNueva.getRazonSocial()
                                + "! Tu cuenta ha sido registrada con éxito.",
                        "ALTA_IMPORTACION",
                        "DONANTE",
                        juridicaNueva.getId(),
                        null);
                restTemplate.postForObject(url, request, Void.class);
            } catch (Exception e) {
                System.err.println("Error notificando alta de donante jurídico: " + e.getMessage());
            }
        }
    }

    private String obtenerEmailVal(List<MedioContacto> contactos) {
        if (contactos == null)
            return null;
        return contactos.stream()
                .filter(c -> c.getTipoContacto() == TipoContacto.EMAIL)
                .map(c -> c.getValor())
                .findFirst()
                .orElse(null);
    }

    private String obtenerTelefonoVal(List<MedioContacto> contactos) {
        if (contactos == null)
            return "";
        return contactos.stream()
                .filter(c -> c.getTipoContacto() == TipoContacto.SMS)
                .map(c -> c.getValor())
                .findFirst()
                .orElse("");
    }

    private void actualizarContactos(List<MedioContacto> contactos, String emailVal, String telefonoVal) {
        if (contactos == null)
            return;

        Optional<MedioContacto> emailOpt = contactos.stream()
                .filter(c -> c.getTipoContacto() == TipoContacto.EMAIL)
                .findFirst();

        if (emailOpt.isPresent()) {
            emailOpt.get().setValor(emailVal);
        } else {
            MedioContacto email = new MedioContacto(emailVal, TipoContacto.EMAIL);
            contactos.add(email);
        }

        Optional<MedioContacto> telOpt = contactos.stream()
                .filter(c -> c.getTipoContacto() == TipoContacto.SMS)
                .findFirst();

        if (telOpt.isPresent()) {
            if (!telefonoVal.isEmpty()) {
                telOpt.get().setValor(telefonoVal);
            } else {
                contactos.remove(telOpt.get());
            }
        } else if (!telefonoVal.isEmpty()) {
            MedioContacto tel = new MedioContacto(telefonoVal, TipoContacto.SMS);
            contactos.add(tel);
        }
    }
}
