package routing.community;

import java.util.*;
import core.*;
import routing.*;

public class TotalInterContactEngine implements RoutingDecisionEngine {

    protected Map<DTNHost, Double> lastDisconnectTimes = new HashMap<>();
    protected Map<DTNHost, List<Double>> interContactHistory = new HashMap<>();
    protected Map<DTNHost, Double> startTimestamps = new HashMap<>();

    public TotalInterContactEngine(Settings s) {
    }

    public TotalInterContactEngine(TotalInterContactEngine e) {
        this.lastDisconnectTimes = new HashMap<DTNHost, Double>();
        this.interContactHistory = new HashMap<DTNHost, List<Double>>();
        this.startTimestamps = new HashMap<DTNHost, Double>();
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        lastDisconnectTimes.put(peer, SimClock.getTime());
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        if (lastDisconnectTimes.containsKey(peer)) {
            double lastDown = lastDisconnectTimes.get(peer);
            double currentTime = SimClock.getTime();
            double gap = currentTime - lastDown;

            if (gap > 0) {
                interContactHistory.putIfAbsent(peer, new LinkedList<Double>());
                interContactHistory.get(peer).add(gap);
            }
        }
    }

    public double getTotalInterContact(DTNHost target) {
        List<Double> gaps = interContactHistory.get(target);
        if (gaps == null || gaps.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double total = 0;
        Iterator<Double> it = gaps.iterator();
        while (it.hasNext()) {
            total += it.next();
        }
        return total;
    }

    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        TotalInterContactEngine de = this.getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost)
            return true;
        
        TotalInterContactEngine otherEngine = this.getOtherDecisionEngine(otherHost);
        DTNHost finalDest = m.getTo();

        double myTotalGap = this.getTotalInterContact(finalDest);
        double otherTotalGap = otherEngine.getTotalInterContact(finalDest);
        return otherTotalGap < myTotalGap;
    }

    private TotalInterContactEngine getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works with DecisionEngineRouter";

        return (TotalInterContactEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
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
        return new TotalInterContactEngine(this);
    }

    public void update(DTNHost thisHost) {
    }
}
