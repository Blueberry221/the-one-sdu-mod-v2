/*
 * @(#)CommunityDetectionReport.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package report;

import java.util.*;

import core.*;
import routing.*;
import routing.community.CentralityDetectionEngine;

/**
 * <p>
 * Reports the local communities at each node whenever the done() method is
 * called. Only those nodes whose router is a DecisionEngineRouter and whose
 * RoutingDecisionEngine implements the
 * routing.community.CommunityDetectionEngine are reported. In this way, the
 * report is able to output the result of any of the community detection
 * algorithms.
 * </p>
 * 
 * @author PJ Dillon, University of Pittsburgh
 */
public class CentralityDetectionReport extends Report {
	public CentralityDetectionReport() {
		init();
	}

	// @Override
	// public void done() {
	// List<DTNHost> nodes = SimScenario.getInstance().getHosts();
	// // List<Set<DTNHost>> centrality = new LinkedList<Set<DTNHost>>();

	// // write("Node_ID\tGlobal_Centrality_History_Per_24h...");

	// for (DTNHost h : nodes) {
	// MessageRouter r = h.getRouter();
	// if (!(r instanceof DecisionEngineRouter))
	// continue;
	// RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
	// if (!(de instanceof CentralityDetectionEngine))
	// continue;
	// CentralityDetectionEngine cd = (CentralityDetectionEngine) de;

	// double nilaiGlobal = cd.getGlobalDegreeCentrality();
	// double nilaiLocal = cd.getLocalDegreeCentrality();

	// // New here
	// int[] history = cd.getGlobalArrayCentrality();

	// StringBuilder sb = new StringBuilder();
	// sb.append("Node ").append(h); // Kolom 1: ID Node

	// if (history != null) {
	// for (int val : history) {
	// sb.append("\t").append(val); // Kolom selanjutnya: Data per 24 jam
	// }
	// }

	// // Cetak ke file
	// write(sb.toString());

	// write("Node " + h + " Nilai Global: " + nilaiGlobal + "\tNilai Local: " +
	// nilaiLocal);
	// }
	// super.done();
	// }

	@Override
	public void done() {
		List<DTNHost> nodes = SimScenario.getInstance().getHosts();

		// Header CSV agar kolom terbaca rapi di Excel
		// Tentukan berapa hari yang ingin kamu buat headernya
		int jumlahHari = 60;

		StringBuilder header = new StringBuilder();
		header.append("Node_ID\tGlobal_Avg\tLocal_Avg");

		// Loop untuk membuat Day_1 sampai Day_60 secara otomatis
		for (int i = 1; i <= jumlahHari; i++) {
			header.append("\tDay_").append(i);
		}

		// Tulis ke file report
		write(header.toString());

		for (DTNHost h : nodes) {
			MessageRouter r = h.getRouter();
			if (!(r instanceof DecisionEngineRouter))
				continue;

			RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
			if (!(de instanceof CentralityDetectionEngine))
				continue;

			CentralityDetectionEngine cd = (CentralityDetectionEngine) de;

			// Ambil data
			double nilaiGlobal = cd.getGlobalDegreeCentrality();
			double nilaiLocal = cd.getLocalDegreeCentrality();
			int[] history = cd.getGlobalArrayCentrality();

			// Susun SATU baris teks (Tab-Separated)
			StringBuilder sb = new StringBuilder();
			sb.append(h.getAddress()).append("\t") // Kolom ID
					.append(nilaiGlobal).append("\t") // Kolom Global Avg
					.append(nilaiLocal); // Kolom Local Avg

			if (history != null) {
				for (int val : history) {
					sb.append("\t").append(val); // Kolom harian
				}
			}

			// HANYA SATU KALI WRITE PER NODE
			write(sb.toString());
		}
		super.done();
	}

}