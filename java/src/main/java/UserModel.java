import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Modelo de datos para usuarios en LP1
 */
public class UserModel {
    private static final Logger logger = Logger.getLogger(UserModel.class.getName());
    private static final Gson gson = new GsonBuilder().create();

    private int id;
    private String requestId;
    private String nombre;
    private String correo;
    private String clave;
    private String dni;
    private String telefono;
    private List<Integer> amigosFrecuentes;

    // Constructor por defecto
    public UserModel() {
        this.amigosFrecuentes = new ArrayList<>();
    }

    // Constructor completo
    public UserModel(String requestId, String nombre, String correo, String clave,
            String dni, String telefono, List<Integer> amigosFrecuentes) {
        this.requestId = requestId;
        this.nombre = nombre;
        this.correo = correo;
        this.clave = clave;
        this.dni = dni;
        this.telefono = telefono;
        this.amigosFrecuentes = amigosFrecuentes != null ? amigosFrecuentes : new ArrayList<>();
    }

    // Método estático para crear desde JSON
    public static UserModel fromJson(String json) {
        try {
            UserModel user = gson.fromJson(json, UserModel.class);

            // Validar campos requeridos
            if (user.getDni() == null || user.getDni().trim().isEmpty()) {
                logger.warning("❌ DNI es requerido");
                return null;
            }

            if (user.getNombre() == null || user.getNombre().trim().isEmpty()) {
                logger.warning("❌ Nombre es requerido");
                return null;
            }

            // Asegurar que amigosFrecuentes no sea null
            if (user.amigosFrecuentes == null) {
                user.amigosFrecuentes = new ArrayList<>();
            }

            return user;

        } catch (JsonSyntaxException e) {
            logger.log(Level.SEVERE, "❌ Error parseando JSON: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error creando UserModel desde JSON: " + e.getMessage(), e);
            return null;
        }
    }

    // Convertir a JSON
    public String toJson() {
        try {
            return gson.toJson(this);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error convirtiendo a JSON: " + e.getMessage(), e);
            return "{}";
        }
    }

    // Validar datos del usuario
    public boolean isValid() {
        return dni != null && !dni.trim().isEmpty() &&
                nombre != null && !nombre.trim().isEmpty() &&
                correo != null && !correo.trim().isEmpty();
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public List<Integer> getAmigosFrecuentes() {
        return amigosFrecuentes;
    }

    public void setAmigosFrecuentes(List<Integer> amigosFrecuentes) {
        this.amigosFrecuentes = amigosFrecuentes != null ? amigosFrecuentes : new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format(
                "UserModel{id=%d, requestId='%s', nombre='%s', correo='%s', dni='%s', telefono='%s', amigos=%s}",
                id, requestId, nombre, correo, dni, telefono, amigosFrecuentes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        UserModel user = (UserModel) obj;
        return dni != null && dni.equals(user.dni);
    }

    @Override
    public int hashCode() {
        return dni != null ? dni.hashCode() : 0;
    }
}