/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class ch_usi_da_paxos_storage_CyclicArray */

#ifndef _Included_ch_usi_da_paxos_storage_CyclicArray
#define _Included_ch_usi_da_paxos_storage_CyclicArray
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     ch_usi_da_paxos_storage_CyclicArray
 * Method:    init
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_ch_usi_da_paxos_storage_CyclicArray_init
  (JNIEnv *, jobject);

/*
 * Class:     ch_usi_da_paxos_storage_CyclicArray
 * Method:    nput
 * Signature: (I[B)V
 */
JNIEXPORT void JNICALL Java_ch_usi_da_paxos_storage_CyclicArray_nput
  (JNIEnv *, jobject, jint, jbyteArray);

/*
 * Class:     ch_usi_da_paxos_storage_CyclicArray
 * Method:    nget
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_ch_usi_da_paxos_storage_CyclicArray_nget
  (JNIEnv *, jobject, jint);

/*
 * Class:     ch_usi_da_paxos_storage_CyclicArray
 * Method:    contains
 * Signature: (Ljava/lang/Integer;)Z
 */
JNIEXPORT jboolean JNICALL Java_ch_usi_da_paxos_storage_CyclicArray_contains
  (JNIEnv *, jobject, jobject);

#ifdef __cplusplus
}
#endif
#endif
