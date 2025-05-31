package src.main.java;

/**
 * Example class used to demonstrate the communication
 * between applications that run on the same vehicle.
 * Uses two example fields to show the general idea.
 */
public final class IntraVehicleMsg {

    /**
     * The originating vehicle.
     * Always the 'own' vehicle in case of intra vehicle communication.
     */
    private final String origin;

    /**
     * Arbitrary id for that message which can be assigned according
     * to the actual needs of the application.
     */
    private final int id;

    public IntraVehicleMsg(String origin, int id) {
        this.origin = origin;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("IntraVehicleMsg{");
        sb.append("origin='").append(origin).append('\'');
        sb.append(", id=").append(id);
        sb.append('}');
        return sb.toString();
    }
}
