javac -classpath c:\Users\whatamidoing\adt-bundle-windows-x86_64-20131030\sdk\platforms\android-19\android.jar;"c:\Users\whatamidoing\OpenCV-2.4.9-android-sdk\sdk\java\bin\opencv library - 2.4.9.jar";..\bin\classes;..\..\android-common\android-support-v4.jar com\watamidoing\nativecamera\*.java

javah -classpath c:\Users\whatamidoing\adt-bundle-windows-x86_64-20131030\sdk\platforms\android-19\android.jar;"c:\Users\whatamidoing\OpenCV-2.4.9-android-sdk\sdk\java\bin\opencv library - 2.4.9.jar";..\bin\classes;..\..\android-common\android-support-v4.jar com.watamidoing.nativecamera.CameraPreviewer

del ..\jni\native\src\com_watamidoing_nativecamera_CameraPreviewer.h
copy com_watamidoing_nativecamera_CameraPreviewer.h ..\jni\native\src\