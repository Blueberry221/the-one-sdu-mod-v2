import pandas as pd
import matplotlib.pyplot as plt
import os
import glob

# --- KONFIGURASI ---
base_path = "reports" 
output_base = os.path.join(base_path, "visualizer")
demos = ["demo1", "demo2"] 

# SESUAI GAMBAR: File diawali 'Testing_' dan berakhiran '.csv'
# Kita mencari MessageStatsReport untuk mengambil data probabilitas dsb.
file_pattern = "SnW_testing_MessageStatsReport.csv" 

target_metrics = [
    'delivery_prob', 
    'overhead_ratio', 
    'latency_avg', 
    'hopcount_avg', 
    'relayed', 
    'delivered'
]

def parse_custom_one_report(file_path):
    """Mengekstrak metrik dari format CSV/Txt The ONE"""
    extracted_data = {}
    if not os.path.exists(file_path):
        return None
        
    with open(file_path, 'r') as f:
        for line in f:
            # Tetap menggunakan logika pemisah ':' karena format CSV ONE 
            # untuk MessageStats sebenarnya adalah teks dengan titik dua
            if ':' in line:
                try:
                    parts = line.split(':')
                    key = parts[0].strip()
                    value = parts[1].strip()
                    if key in target_metrics:
                        extracted_data[key] = float(value)
                except (ValueError, IndexError):
                    continue
    return extracted_data

def create_dir(path):
    if not os.path.exists(path):
        os.makedirs(path)

print("--- Memulai Analisis Laporan (Mode CSV) ---")

all_summaries = []

for d in demos:
    input_path = os.path.join(base_path, d, file_pattern)
    
    if os.path.exists(input_path):
        print(f"Mengekstrak data dari: {input_path}")
        stats = parse_custom_one_report(input_path)
        
        if stats:
            stats['Scenario'] = d
            all_summaries.append(stats)
            print(f"   -> Berhasil mengambil {len(stats)} metrik.")
    else:
        # Jika file Testing_MessageStatsReport.csv belum ada
        print(f"Peringatan: File {file_pattern} TIDAK ditemukan di folder {d}")
        print(f"Pastikan sudah menambahkan MessageStatsReport di .settings")

# --- VISUALISASI ---
if all_summaries:
    df_all = pd.DataFrame(all_summaries)
    
    # Mapping Router (Sesuaikan dengan skenario kamu)
    router_map = {"demo1": "SnW_Classic", "demo2": "SnW_Engine", "demo3": "SnF"}
    df_all['Router'] = df_all['Scenario'].map(router_map)
    
    comparison_dir = os.path.join(output_base, "global_comparison")
    create_dir(comparison_dir)
    
    # Plotting
    metrics_to_plot = ['delivery_prob', 'overhead_ratio', 'latency_avg']
    fig, axes = plt.subplots(1, 3, figsize=(18, 6))
    
    for i, m in enumerate(metrics_to_plot):
        if m in df_all.columns:
            axes[i].bar(df_all['Router'], df_all[m], color=['#3498db', '#9b59b6', '#e74c3c'], alpha=0.8)
            axes[i].set_title(f"Perbandingan {m.replace('_', ' ').title()}")
            axes[i].set_ylabel("Nilai")
    
    plt.tight_layout()
    plt.savefig(os.path.join(comparison_dir, "grafik_testing.png"))
    plt.show()
    
    print("\n--- HASIL TABEL ---")
    print(df_all[['Router', 'delivery_prob', 'overhead_ratio', 'latency_avg']])
else:
    print("\n[ERROR] Data tidak ditemukan. Periksa kembali folder reports kamu.")