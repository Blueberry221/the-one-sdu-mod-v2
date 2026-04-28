package routing.community;

import java.util.*;
import core.*;
import routing.*;

public class FrequencyEngine implements RoutingDecisionEngine {

    protected Map<DTNHost, Integer> contactFrequency = new HashMap<>();

    public FrequencyEngine(Settings s) {
    }

    public FrequencyEngine(FrequencyEngine e) {
        this.contactFrequency = new HashMap<>(e.contactFrequency);
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        int count = contactFrequency.getOrDefault(peer, 0);

        contactFrequency.put(peer, count + 1);
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    public int getContactFrequency(DTNHost target) {
        return contactFrequency.getOrDefault(target, 0);
    }

    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost)
            return true;

        FrequencyEngine otherEngine = this.getOtherDecisionEngine(otherHost);
        DTNHost finalDest = m.getTo();

        int myFreq = this.getContactFrequency(finalDest);
        int otherFreq = otherEngine.getContactFrequency(finalDest);

        return otherFreq > myFreq;
    }

    private FrequencyEngine getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works with DecisionEngineRouter";

        return (FrequencyEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    public boolean newMessage(Message m) {
        return true;
    }

    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
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
        return new FrequencyEngine(this);
    }

    public void update(DTNHost thisHost) {
    }

}
