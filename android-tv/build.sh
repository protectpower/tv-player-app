# Builds the APK. Run from android-tv/. Needs JDK 17, zip, and Android build-tools 34 + platform 34.
set -e
BT=${BT:-/tmp/sdk/android-14}; AJ=${AJ:-/tmp/sdk/android-34/android.jar}
mkdir -p src/assets && cp ../public/tv.html src/assets/index.html
rm -rf out && mkdir -p out/cls out/dex
$BT/aapt2 compile --dir src/res -o out/res.zip
$BT/aapt2 link -o out/base.apk -I $AJ --manifest src/AndroidManifest.xml -A src/assets out/res.zip --auto-add-overlay
javac -source 8 -target 8 -bootclasspath $AJ -classpath $AJ -d out/cls src/java/com/cplayer/tv/MainActivity.java
$BT/d8 --lib $AJ --min-api 21 --output out/dex $(find out/cls -name '*.class')
cp out/base.apk out/unsigned.apk && (cd out/dex && zip -q ../unsigned.apk classes.dex)
$BT/zipalign -f 4 out/unsigned.apk out/aligned.apk
$BT/apksigner sign --ks key.jks --ks-pass pass:${KS_PASS:-cplayer123} --out out/ProtectPowerTV.apk out/aligned.apk
$BT/apksigner verify out/ProtectPowerTV.apk && ls -la out/ProtectPowerTV.apk
