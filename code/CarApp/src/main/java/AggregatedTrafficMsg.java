import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;

public class AggregatedTrafficMsg extends V2xMessage {

    private final int route0VehicleCount;
    private final int route1VehicleCount;
    // Optional: add average speeds
    // private final double route0AvgSpeed;
    // private final double route1AvgSpeed;

    public AggregatedTrafficMsg(MessageRouting routing, int route0VehicleCount, int route1VehicleCount) {
        super(routing);
        this.route0VehicleCount = route0VehicleCount;
        this.route1VehicleCount = route1VehicleCount;
    }

    public int getRoute0VehicleCount() {
        return route0VehicleCount;
    }

    public int getRoute1VehicleCount() {
        return route1VehicleCount;
    }

    // Optionally add getters for average speeds if you extend the constructor

    @Override
    public EncodedPayload getPayload() {
        // Return null or an appropriate payload object as needed
        return null;
    }
}
