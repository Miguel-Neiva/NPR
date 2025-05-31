package src.main.java;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;


public final class GreenWaveMsg extends V2xMessage {
    private final String segredo;
    private final String rota;
    private final String id_carro;
    private final EncodedPayload payload;
    private final static long    MIN_LEN = 8L;

    public GreenWaveMsg(MessageRouting routing, String segredo, String rota, String id_carro) {
        super(routing);
        this.segredo = segredo;
        this.rota = rota;
        this.id_carro = id_carro;
        String message = segredo + rota + id_carro;
        payload = new EncodedPayload(message.length(), MIN_LEN);
    }

    public String getSegredo() {
        return segredo;
    }

    public String getRota() {
        return rota;
    }

    public String getId_carro() {
        return id_carro;
    }

    public String getMessage() {
        return segredo + " | " + rota + " | " + id_carro;
    }

    public EncodedPayload getPayload() {
        return null;
    }


    public EncodedPayload getPayLoad() {
        return null;
    }

    public String toString() {
        return this.getMessage();
    }
}
