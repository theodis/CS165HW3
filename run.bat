rem SET ClassPath=%ClassPath%;T:\javagaming\graphicslib3D\graphicslib3D.jar;T:\javagaming\jbullet\jbullet.jar;T:\javagaming\jinput\jinput.jar;T:\javagaming\jogl\jogl-all.jar;T:\javagaming\jogl\gluegen-rt.jar;T:\javagaming\jogl\joal.jar;T:\javagaming\luaj\luaj-jme-3.0.jar;T:\javagaming\ode4j\core-0.3.0.jar;T:\javagaming\sage\sage.jar;T:\javagaming\vecmath\vecmath.jar
cd src
rem java -Djava.library.path=T:/javagaming/jinput/lib/ -Dsun.java2d.d3d=false a3.Starter 173.255.254.134 1234
java -Dsun.java2d.d3d=false a3.Starter 173.255.254.134 1234
cd ..
