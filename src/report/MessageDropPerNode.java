package report;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class MessageDropPerNode extends Report implements MessageListener {
    private Map<DTNHost, Integer> dropCountPerNode;
    private Map<DTNHost, Integer> deletedCountPerNode;
    private List<Double> msgBufferTime;

    private int nrofDropped;
    private int nrofRemoved;

    public MessageDropPerNode() {
        init();
    }

    @Override
    protected void init() {
        this.dropCountPerNode = new HashMap<DTNHost, Integer>();
        this.deletedCountPerNode = new HashMap<DTNHost, Integer>();
        this.msgBufferTime = new java.util.ArrayList<Double>();
        this.nrofDropped = 0;
        this.nrofRemoved = 0;
        super.init();
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId()))
            return;

        // Tambahkan Map untuk menyimpan total kejadian per node
        Map<DTNHost, Integer> targetMap = dropped ? dropCountPerNode : deletedCountPerNode;

        if (dropped)
            this.nrofDropped++;
        else
            this.nrofRemoved++;

        int count = targetMap.containsKey(where) ? targetMap.get(where) : 0;
        targetMap.put(where, count + 1);

        // Hitung waktu tinggal di buffer (Optional: pastikan list sudah di-init)
        double stayTime = getSimTime() - m.getReceiveTime();
        this.msgBufferTime.add(stayTime);
    }

    @Override
    public void newMessage(Message m) {
        // do nothing
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        // do nothing
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        // do nothing
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to,
            boolean firstDelivery) {
        // do nothing
    }

    @Override
    public void done() {
        write("Message stats for scenario " + getScenarioName());
        write("sim_time: " + format(getSimTime()));
        write("Total messages dropped: " + this.nrofDropped);
        write("Total messages removed: " + this.nrofRemoved);

        write("\n--- Dropped count per node ---");
        // Mengurutkan output berdasarkan ID node agar enak dibaca
        for (DTNHost host : dropCountPerNode.keySet()) {
            write("Node " + host.getAddress() + ": " + dropCountPerNode.get(host));
        }

        // Hitung rata-rata waktu di buffer jika diperlukan
        if (!msgBufferTime.isEmpty()) {
            double sum = 0;
            for (Double t : msgBufferTime)
                sum += t;
            write("\nAverage buffer stay time: " + format(sum / msgBufferTime.size()));
        }

        super.done();
    }
}
