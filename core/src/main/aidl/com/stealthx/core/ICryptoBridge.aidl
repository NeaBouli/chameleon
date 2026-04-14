// ICryptoBridge.aidl
// Chameleon — IPC interface between AccessibilityService and CryptoService
// The Accessibility Service (untrusted context) communicates with
// CryptoService (:crypto process) exclusively via this AIDL interface.
// NO crypto logic may exist in the AccessibilityService itself.

package com.stealthx.core;

// Structured parcel for process text results
parcelable ProcessTextResult;

interface ICryptoBridge {

    // Process text: encrypt or decrypt depending on current security level
    // packageName is used as AAD (Additional Authenticated Data) for XChaCha20
    ProcessTextResult processText(String text, String packageName);

    // Notify CryptoService of current security level change
    void notifySecurityLevel(int level);

    // Health check — returns true if CryptoService is ready
    boolean isReady();
}
