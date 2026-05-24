/* * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Report khusus untuk mengukur performa pengiriman pesan ASLI saja.
 * Semua pesan tanda terima (Active Receipt) yang diawali dengan "R_" / bisa custom
 * akan difilter dan diabaikan dari perhitungan metrik.
 */
public class FilteredMessageStatsReport extends Report implements MessageListener {
    private Map<String, Double> creationTimes;
    private List<Double> latencies;
    private List<Integer> hopCounts;
    private List<Double> msgBufferTime;
    private List<Double> rtt; // round trip times
    
    private int nrofDropped;
    private int nrofRemoved;
    private int nrofStarted;
    private int nrofAborted;
    private int nrofRelayed;
    private int nrofCreated;
    private int nrofResponseReqCreated;
    private int nrofResponseDelivered;
    private int nrofDelivered;
    
    public FilteredMessageStatsReport() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.creationTimes = new HashMap<String, Double>();
        this.latencies = new ArrayList<Double>();
        this.msgBufferTime = new ArrayList<Double>();
        this.hopCounts = new ArrayList<Integer>();
        this.rtt = new ArrayList<Double>();
        
        this.nrofDropped = 0;
        this.nrofRemoved = 0;
        this.nrofStarted = 0;
        this.nrofAborted = 0;
        this.nrofRelayed = 0;
        this.nrofCreated = 0;
        this.nrofResponseReqCreated = 0;
        this.nrofResponseDelivered = 0;
        this.nrofDelivered = 0;
    }

    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId()) || m.getId().startsWith("R_")) {
            return; // Filter pesan obat
        }
        
        if (dropped) {
            this.nrofDropped++;
        }
        else {
            this.nrofRemoved++;
        }
        
        this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
    }

    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        if (isWarmupID(m.getId()) || m.getId().startsWith("R_")) {
            return; // Filter pesan obat
        }
        
        this.nrofAborted++;
    }

    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
        if (isWarmupID(m.getId()) || m.getId().startsWith("R_")) {
            return; // Filter pesan obat
        }

        this.nrofRelayed++;
        if (finalTarget) {
            this.latencies.add(getSimTime() - this.creationTimes.get(m.getId()));
            this.nrofDelivered++;
            this.hopCounts.add(m.getHops().size() - 1);

            if (m.isResponse()) {
                this.rtt.add(getSimTime() - m.getRequest().getCreationTime());
                this.nrofResponseDelivered++;
            }
        }
    }

    public void newMessage(Message m) {
        if (isWarmup()) {
            addWarmupID(m.getId());
            return;
        }
        if (m.getId().startsWith("R_")) {
            return; // Filter pesan obat agar tidak menambah counter 'created'
        }
        
        this.creationTimes.put(m.getId(), getSimTime());
        this.nrofCreated++;
        if (m.getResponseSize() > 0) {
            this.nrofResponseReqCreated++;
        }
    }
    
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        if (isWarmupID(m.getId()) || m.getId().startsWith("R_")) {
            return; // Filter pesan obat
        }

        this.nrofStarted++;
    }
    

    @Override
    public void done() {
        write("Filtered Message stats for scenario " + getScenarioName() + 
                "\nsim_time: " + format(getSimTime()));
        double deliveryProb = 0; 
        double responseProb = 0; 
        double overHead = Double.NaN;   
        
        if (this.nrofCreated > 0) {
            deliveryProb = (1.0 * this.nrofDelivered) / this.nrofCreated;
        }
        if (this.nrofDelivered > 0) {
            overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) / this.nrofDelivered;
        }
        if (this.nrofResponseReqCreated > 0) {
            responseProb = (1.0 * this.nrofResponseDelivered) / this.nrofResponseReqCreated;
        }
        
        String statsText = "\ncreated: " + this.nrofCreated + 
            "\nstarted: " + this.nrofStarted + 
            "\nrelayed: " + this.nrofRelayed +
            "\naborted: " + this.nrofAborted +
            "\ndropped: " + this.nrofDropped +
            "\nremoved: " + this.nrofRemoved +
            "\ndelivered: " + this.nrofDelivered +
            "\ndelivery_prob: " + format(deliveryProb) +
            "\nresponse_prob: " + format(responseProb) + 
            "\noverhead_ratio: " + format(overHead) + 
            "\nlatency_avg: " + getAverage(this.latencies) +
            "\nlatency_med: " + getMedian(this.latencies) + 
            "\nhopcount_avg: " + getIntAverage(this.hopCounts) +
            "\nhopcount_med: " + getIntMedian(this.hopCounts) + 
            "\nbuffertime_avg: " + getAverage(this.msgBufferTime) +
            "\nbuffertime_med: " + getMedian(this.msgBufferTime) +
            "\nrtt_avg: " + getAverage(this.rtt) +
            "\nrtt_med: " + getMedian(this.rtt)
            ;
        
        write(statsText);
        super.done();
    }
}