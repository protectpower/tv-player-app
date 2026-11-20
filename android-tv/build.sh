set -e
BT=/tmp/sdk/android-14; AJ=/tmp/sdk/android-34/android.jar
rm -rf out && mkdir -p out/cls out/dex
$BT/aapt2 compile --dir src/res -o out/res.zip
$BT/aapt2 link -o out/base.apk -I $AJ --manifest src/AndroidManifest.xml -A src/assets out/res.zip --auto-add-overlay
javac -source 8 -target 8 -bootclasspath $AJ -classpath $AJ -d out/cls src/java/com/cplayer/tv/MainActivity.java 2>&1 | grep -v warning || true
$BT/d8 --lib $AJ --min-api 21 --output out/dex $(find out/cls -name '*.class')
cp out/base.apk out/unsigned.apk && (cd out/dex && zip -q ../unsigned.apk classes.dex)
$BT/zipalign -f 4 out/unsigned.apk out/aligned.apk
[ -f key.jks ] || keytool -genkeypair -keystore key.jks -storepass cplayer123 -keypass cplayer123 -alias cplayer -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=cPlayer TV"
$BT/apksigner sign --ks key.jks --ks-pass pass:cplayer123 --out out/cPlayerTV.apk out/aligned.apk
$BT/apksigner verify out/cPlayerTV.apk && ls -la out/cPlayerTV.apk
