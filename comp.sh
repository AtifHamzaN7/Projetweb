# Compilation des fichiers Java avant de copier dans tmp
javac -d $1/bin -cp "$1/lib/*" $1/src/pack/*.java
mkdir -p tmp/WEB-INF/classes
cp -r $1/bin/pack tmp/WEB-INF/classes/.
cp -r $1/lib tmp/WEB-INF/.
cp -r $1/src/webcontent/* tmp/.
jar cf $1.war -C tmp .
rm -rf tmp
