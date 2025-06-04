import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;

public class FogMetricsMsg extends V2xMessage {
    private final double avgR0;
    private final double avgR1;

    public FogMetricsMsg(MessageRouting routing, double avgR0, double avgR1) {
        super(routing);
        this.avgR0 = avgR0;
        this.avgR1 = avgR1;
    }

    public double getAvgR0() { return avgR0; }
    public double getAvgR1() { return avgR1; }

    @Override
    public EncodedPayload getPayload() {
        return null;
    }
}