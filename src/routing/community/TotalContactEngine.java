package routing.community;

import java.util.*;
import core.*;
import routing.*;

public class TotalContactEngine implements RoutingDecisionEngine {

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;

    public TotalContactEngine(Settings s) {
    }

    public TotalContactEngine(TotalContactEngine e) {
        //inisiasi timespamps dan history koneksi
        startTimestamps = new HashMap<DTNHost, Double>();
        connHistory = new HashMap<DTNHost, List<Duration>>();
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double time = cek(thisHost, peer);
        double etime = SimClock.getTime();

        //list untuk menyimpan history koneksi
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else {
            history = connHistory.get(peer);
        }

        //jika durasi koneksi lebih dari 0, maka simpan ke history
        if (etime - time > 0) {
            history.add(new Duration(time, etime));
        }

        startTimestamps.remove(peer);
    }

    public double cek(DTNHost thisHost, DTNHost peer) {
        if (startTimestamps.containsKey(peer)) {
            return startTimestamps.get(peer);
        }
        return SimClock.getTime();
    }

    public double getTotalContactDuration(DTNHost target) {
        //ambil history koneksi dengan target
        List<Duration> history = connHistory.get(target);
        if (history == null)
            return 0.0;

        double total = 0;
        //hitung total durasi koneksi dengan target menggunakan iterator
        Iterator<Duration> it = history.iterator();
        //jika masih ada history koneksi, tambahkan durasi koneksi ke total
        while (it.hasNext()) {
            //durasi koneksi saat ini
            Duration d = it.next();
            //total durasi koneksi dengan target adalah total durasi koneksi sebelumnya ditambah durasi koneksi saat ini
            total += (d.end - d.start);
        }

        return total;
    }

    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        TotalContactEngine de = this.getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost)
            return true;

        TotalContactEngine otherEngine = this.getOtherDecisionEngine(otherHost);
        DTNHost finalDest = m.getTo();

        //cek durasi host
        double myDuration = this.getTotalContactDuration(finalDest);
        //cek durasi host lain
        double otherDuration = otherEngine.getTotalContactDuration(finalDest);

        //jika durasi host lain lebih besar dari durasi host ini, maka kirim pesan ke host lain
        return otherDuration > myDuration;
    }

    private TotalContactEngine getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (TotalContactEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    public boolean newMessage(Message m) {
        return true;
    }

    public boolean isFinalDest(Message m, DTNHost aHost) {
        return true;
    }

    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return true;
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost thisHost) {
        return true;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost thisHost) {
        return true;
    }

    public RoutingDecisionEngine replicate() {
        return new TotalContactEngine(this);
    }

    public void update(DTNHost thisHost) {
    }
}
