#include <jni.h>
#include <pwd.h>
#include <grp.h>
#include <sys/types.h>

JNIEXPORT jobject JNICALL
Java_com_klyx_native_Os_getpwuid(JNIEnv *env, jclass clazz, jint uid) {
    struct passwd *pw = getpwuid((uid_t) uid);
    if (pw == NULL) {
        return NULL;
    }

    jclass pwClass = (*env)->FindClass(env, "com/klyx/native/Passwd");
    if (pwClass == NULL) {
        return NULL;
    }

    jmethodID constructor = (*env)->GetMethodID(env, pwClass, "<init>",
            "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;)V");

    jstring name = (*env)->NewStringUTF(env, pw->pw_name);
    jstring dir = (*env)->NewStringUTF(env, pw->pw_dir);
    jstring shell = (*env)->NewStringUTF(env, pw->pw_shell);

    jobject obj = (*env)->NewObject(env, pwClass, constructor, name, (jint) pw->pw_uid, (jint) pw->pw_gid, dir, shell);

    (*env)->DeleteLocalRef(env, name);
    (*env)->DeleteLocalRef(env, dir);
    (*env)->DeleteLocalRef(env, shell);
    (*env)->DeleteLocalRef(env, pwClass);

    return obj;
}

JNIEXPORT jobject JNICALL
Java_com_klyx_native_Os_getgrgid(JNIEnv *env, jclass clazz, jint gid) {
    struct group *gr = getgrgid((gid_t) gid);
    if (gr == NULL) {
        return NULL;
    }

    jclass groupClass = (*env)->FindClass(env, "com/klyx/native/Group");
    if (groupClass == NULL) {
        return NULL;
    }

    jmethodID constructor = (*env)->GetMethodID(
            env,
            groupClass,
            "<init>",
            "(Ljava/lang/String;I)V");

    if (constructor == NULL) {
        (*env)->DeleteLocalRef(env, groupClass);
        return NULL;
    }

    jstring name = (*env)->NewStringUTF(env, gr->gr_name);

    jobject obj = (*env)->NewObject(
            env,
            groupClass,
            constructor,
            name,
            (jint) gr->gr_gid);

    (*env)->DeleteLocalRef(env, name);
    (*env)->DeleteLocalRef(env, groupClass);

    return obj;
}
