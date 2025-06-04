import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;

public class AggregatedTrafficMsg extends V2xMessage {

    private final int route0VehicleCount;
    private final int route1VehicleCount;
    private final double route0AvgSpeed;
    private final double route1AvgSpeed;

    public AggregatedTrafficMsg(MessageRouting routing, int route0VehicleCount, int route1VehicleCount,
                                double route0AvgSpeed, double route1AvgSpeed) {
        super(routing);
        this.route0VehicleCount = route0VehicleCount;
        this.route1VehicleCount = route1VehicleCount;
        this.route0AvgSpeed = route0AvgSpeed;
        this.route1AvgSpeed = route1AvgSpeed;
    }

    public int getRoute0VehicleCount() {
        return route0VehicleCount;
    }

    public int getRoute1VehicleCount() {
        return route1VehicleCount;
    }

    public double getRoute0AvgSpeed() {
        return route0AvgSpeed;
    }

    public double getRoute1AvgSpeed() {
        return route1AvgSpeed;
    }

    @Override
    public EncodedPayload getPayload() {
        // Return null or an appropriate payload object as needed
        return null;
    }

    @Override
    public String toString() {
        return "AggregatedTrafficMsg{" +
                "route0VehicleCount=" + route0VehicleCount +
                ", route1VehicleCount=" + route1VehicleCount +
                ", route0AvgSpeed=" + route0AvgSpeed +
                ", route1AvgSpeed=" + route1AvgSpeed +
                '}';
    }

}