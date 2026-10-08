import sys
import os
import cv2
import socket
import time
import threading

# Silence low-level warnings
sys.stderr = open(os.devnull, 'w')

LIMELIGHT_STREAM_URL = "http://192.168.43.1:5800"
PORT = 6000
SAVE_DIR = os.path.abspath("./dataset_raw_images")
os.makedirs(SAVE_DIR, exist_ok=True)

# Shared variables
take_snapshot_flag = False

# Async File Saver
def save_image_async(filename, frame):
    cv2.imwrite(filename, frame)
    print(f"[!] SUCCESS! Saved High-Res Image: {os.path.basename(filename)}", flush=True)

# Background TCP Listener
def socket_listener():
    global take_snapshot_flag

    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind(("0.0.0.0", PORT))
    server_socket.listen(1)

    print(f"\n[+] TCP Server running on port {PORT}.")
    print("Ready! Waiting for robot connection...\n")

    while True:
        client_socket, addr = server_socket.accept()
        print(f"[+] Robot connected from {addr[0]}!", flush=True)

        while True:
            try:
                data = client_socket.recv(1024)
                if not data:
                    break

                msg = data.decode().strip()
                if "SNAP" in msg:
                    take_snapshot_flag = True

            except (ConnectionResetError, BrokenPipeError):
                break

        client_socket.close()

# Start Network Listener
threading.Thread(target=socket_listener, daemon=True).start()

# Connect to Camera and FORCE HIGH RESOLUTION
print(f"Connecting to Limelight Stream at {LIMELIGHT_STREAM_URL}...")
cap = cv2.VideoCapture(LIMELIGHT_STREAM_URL, cv2.CAP_FFMPEG)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)

# Request Max Resolution (Limelight will provide the highest it supports for the pipeline)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 960)

try:
    while True:
        ret, frame = cap.read()
        if not ret:
            continue

        if take_snapshot_flag:
            take_snapshot_flag = False

            timestamp = time.strftime("%Y%m%d_%H%M%S") + f"_{int(time.time() * 1000) % 1000:03d}"
            filename = os.path.join(SAVE_DIR, f"train_img_{timestamp}.png")

            frame_copy = frame.copy()
            threading.Thread(target=save_image_async, args=(filename, frame_copy)).start()

except KeyboardInterrupt:
    print("\n[!] Server stopped.")
finally:
    cap.release()