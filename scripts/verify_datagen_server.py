"""Boot a real dedicated server and fail on recipe decode errors, then shut it down."""
from pathlib import Path
import subprocess
import threading
import time

directory = Path("run/server")
directory.mkdir(parents=True, exist_ok=True)
(directory / "eula.txt").write_text("eula=true\n")
(directory / "server.properties").write_text("online-mode=false\nserver-port=0\n")
process = subprocess.Popen(["./gradlew", "runServer", "--console=plain"],
                           stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                           stderr=subprocess.STDOUT, text=True, bufsize=1)
ready = threading.Event()
lines = []
def output():
    for line in process.stdout:
        print(line, end="", flush=True)
        lines.append(line)
        if 'For help, type "help"' in line:
            ready.set()
            process.stdin.write("stop\n")
            process.stdin.flush()
thread = threading.Thread(target=output, daemon=True)
thread.start()
deadline = time.monotonic() + 240
while process.poll() is None and time.monotonic() < deadline:
    time.sleep(1)
if process.poll() is None:
    process.terminate()
    try:
        process.wait(timeout=15)
    except subprocess.TimeoutExpired:
        process.kill()
    raise RuntimeError("Server failed to boot and stop within four minutes")
thread.join(timeout=5)
assert ready.is_set(), "Dedicated server did not reach ready state"
assert process.returncode == 0, f"Server exited with {process.returncode}"
errors = [line for line in lines if "Parsing error loading recipe" in line or "Failed to parse recipe" in line]
assert not errors, "".join(errors)
