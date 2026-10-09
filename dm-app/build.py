"""Build/test a dependency-free Android APK with JDK 17 and SDK 35 tools.

Set JAVA_HOME, ANDROID_HOME, DM_KEYSTORE and DM_STORE_PASSWORD.
Private signing material must stay outside this repository.
"""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

root = Path(__file__).resolve().parent
out = root / 'build'
out.mkdir(exist_ok=True)
java = Path(os.environ['JAVA_HOME']) / 'bin'
sdk = Path(os.environ['ANDROID_HOME'])
tools = sdk / 'build-tools' / '35.0.0'
android = sdk / 'platforms' / 'android-35' / 'android.jar'
suffix = '.exe' if os.name == 'nt' else ''

def run(*args):
    subprocess.run([str(a) for a in args], check=True)

def j(name):
    return java / (name + suffix)

classes = out / 'classes'
classes.mkdir(exist_ok=True)
sources = list((root / 'src').rglob('*.java'))
run(j('javac'), '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-classpath', android, '-d', classes, *sources)
run(j('javac'), '-encoding', 'UTF-8', '-classpath', classes, '-d', classes, root / 'test' / 'NavigationPolicyTest.java')
run(j('java'), '-classpath', classes, 'NavigationPolicyTest')
# Keep the host-side test class out of the APK.
archive = out / 'app-classes.jar'
with zipfile.ZipFile(archive, 'w') as z:
    for file in (classes / 'uk').rglob('*.class'):
        z.write(file, file.relative_to(classes).as_posix())
run(j('java'), '-cp', tools / 'lib' / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '24', '--lib', android, '--output', out, archive)
resources = out / 'resources.zip'
run(tools / ('aapt2' + suffix), 'compile', '--dir', root / 'res', '-o', resources)
unsigned = out / 'unsigned.apk'
run(tools / ('aapt2' + suffix), 'link', '-I', android, '--manifest', root / 'AndroidManifest.xml', '-A', root / 'assets', '-o', unsigned, resources)
with zipfile.ZipFile(unsigned, 'a', compression=zipfile.ZIP_DEFLATED) as z:
    z.write(out / 'classes.dex', 'classes.dex')
aligned = out / 'aligned.apk'
run(tools / ('zipalign' + suffix), '-f', '-p', '4', unsigned, aligned)
apk = out / 'focusgram-dms.apk'
signer = tools / 'lib' / 'apksigner.jar'
run(j('java'), '-jar', signer, 'sign', '--ks', os.environ['DM_KEYSTORE'], '--ks-key-alias', 'focusgram-dms', '--ks-pass', 'env:DM_STORE_PASSWORD', '--key-pass', 'env:DM_STORE_PASSWORD', '--out', apk, aligned)
run(j('java'), '-jar', signer, 'verify', '--verbose', '--print-certs', apk)
run(tools / ('zipalign' + suffix), '-c', '-v', '4', apk)
print('Built:', apk)

if '--instrumentation' in sys.argv:
    test_classes = out / 'test-classes'
    test_classes.mkdir(exist_ok=True)
    run(j('javac'), '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-classpath', android, '-d', test_classes, root / 'android-test' / 'SmokeTest.java')
    test_jar = out / 'test-classes.jar'
    with zipfile.ZipFile(test_jar, 'w') as z:
        for file in test_classes.rglob('*.class'):
            z.write(file, file.relative_to(test_classes).as_posix())
    test_dex = out / 'test-dex'
    test_dex.mkdir(exist_ok=True)
    run(j('java'), '-cp', tools / 'lib' / 'd8.jar', 'com.android.tools.r8.D8', '--min-api', '24', '--lib', android, '--output', test_dex, test_jar)
    test_unsigned = out / 'test-unsigned.apk'
    run(tools / ('aapt2' + suffix), 'link', '-I', android, '--manifest', root / 'android-test' / 'AndroidManifest.xml', '-o', test_unsigned)
    with zipfile.ZipFile(test_unsigned, 'a', compression=zipfile.ZIP_DEFLATED) as z:
        z.write(test_dex / 'classes.dex', 'classes.dex')
    test_aligned = out / 'test-aligned.apk'
    run(tools / ('zipalign' + suffix), '-f', '4', test_unsigned, test_aligned)
    run(j('java'), '-jar', signer, 'sign', '--ks', os.environ['DM_KEYSTORE'], '--ks-key-alias', 'focusgram-dms', '--ks-pass', 'env:DM_STORE_PASSWORD', '--key-pass', 'env:DM_STORE_PASSWORD', '--out', out / 'smoke-test.apk', test_aligned)
