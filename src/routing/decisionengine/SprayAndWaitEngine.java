package routing.decisionengine;

import core.*;
import routing.*;

public class SprayAndWaitEngine implements RoutingDecisionEngine {

    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SprayAndWaitEngine";
    public static final String MSG_COUNT_PROP = "SprayAndWocus.copies";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." +
            "copies";

    protected int initialNrofCopies;
    protected boolean isBinary;

    public SprayAndWaitEngine(Settings s) {
        Settings snw = new Settings(SPRAYANDWAIT_NS);
        initialNrofCopies = snw.getInt(NROF_COPIES);
        isBinary = snw.getBoolean(BINARY_MODE);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        // TODO Auto-generated method stub

    }

@Override
public boolean newMessage(Message m) {
    m.addProperty(MSG_COUNT_PROP, initialNrofCopies);
    return true;
}

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo().equals(aHost);
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);

        if (nrofCopies != null) {
            if (isBinary) {
                // Penerima mendapat ceil(n/2)
                nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
            } else {
                // Mode normal: penerima hanya dapat 1 copy
                nrofCopies = 1;
            }
            m.updateProperty(MSG_COUNT_PROP, nrofCopies);
        }
        return true; // Simpan pesan di buffer
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        // Cek apakah ini tujuan akhir
        if (m.getTo().equals(otherHost)) {
            return true;
        }

        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        // Jika copy > 1, maka masih dalam fase SPRAY
        return (nrofCopies != null && nrofCopies > 1);
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);

        if (nrofCopies != null && nrofCopies > 1) {
            if (isBinary) {
                // Pengirim menyisakan floor(n/2)
                nrofCopies /= 2;
            } else {
                // Mode normal: kurangi 1
                nrofCopies--;
            }
            m.updateProperty(MSG_COUNT_PROP, nrofCopies);
        }

        // Return false agar pesan TIDAK dihapus 
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    // Constructor untuk replicate
    protected SprayAndWaitEngine(SprayAndWaitEngine e) {
        this.initialNrofCopies = e.initialNrofCopies;
        this.isBinary = e.isBinary;
    }

    public RoutingDecisionEngine replicate() {
        return new SprayAndWaitEngine(this);
    }

}
