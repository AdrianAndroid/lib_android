/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.biometric.integration.testapp

import android.annotation.SuppressLint
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * A message payload to be encrypted by the application.
 */
internal const val PAYLOAD = "hello"

/**
 * A name used to refer to an app-specified secret key.
 */
private const val KEY_NAME = "mySecretKey"

/**
 * The name of the Android keystore provider instance.
 */
private const val KEYSTORE_INSTANCE = "AndroidKeyStore"

/**
 * Returns a [BiometricPrompt.CryptoObject] for crypto-based authentication, which can be configured
 * to [allowBiometricAuth] and/or [allowDeviceCredentialAuth].
 */
@Suppress("DEPRECATION")
@SuppressLint("TrulyRandom")
@RequiresApi(Build.VERSION_CODES.M)
internal fun createCryptoObject(
    allowBiometricAuth: Boolean,
    allowDeviceCredentialAuth: Boolean
): BiometricPrompt.CryptoObject {
    // Create a spec for the key to be generated.
    val keyPurpose = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    val keySpec = Api23Impl.createKeyGenParameterSpecBuilder(KEY_NAME, keyPurpose).run {
        Api23Impl.setBlockModeCBC(this)
        Api23Impl.setEncryptionPaddingPKCS7(this)
        Api23Impl.setUserAuthenticationRequired(this, true)

        // Require authentication for each use of the key.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Api30Impl.setUserAuthenticationParameters(
                this,
                timeout = 0,
                allowBiometricAuth,
                allowDeviceCredentialAuth
            )
        } else {
            Api23Impl.setUserAuthenticationValidityDurationSeconds(this, -1)
        }

        Api23Impl.buildKeyGenParameterSpec(this)
    }

    // Generate and store the key in the Android keystore.
    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_INSTANCE).run {
        init(keySpec)
        generateKey()
    }

    // Prepare the crypto object to use for authentication.
    val cipher = getCipher().apply { init(Cipher.ENCRYPT_MODE, getSecretKey()) }
    return BiometricPrompt.CryptoObject(cipher)
}

/**
 * Returns the cipher that will be used for encryption.
 */
@RequiresApi(Build.VERSION_CODES.M)
private fun getCipher(): Cipher {
    return Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_CBC + "/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7
    )
}

/**
 * Returns the previously generated secret key from keystore.
 */
private fun getSecretKey(): SecretKey {
    val keyStore = KeyStore.getInstance(KEYSTORE_INSTANCE).apply { load(null) }
    return keyStore.getKey(KEY_NAME, null) as SecretKey
}

/**
 * Nested class to avoid verification errors for methods introduced in Android 11 (API 30).
 */
@RequiresApi(Build.VERSION_CODES.R)
private object Api30Impl {
    @DoNotInline
    fun setUserAuthenticationParameters(
        builder: KeyGenParameterSpec.Builder,
        timeout: Int,
        allowBiometricAuth: Boolean,
        allowDeviceCredentialAuth: Boolean
    ) {
        // Set the key type according to the allowed auth types.
        var keyType = 0
        if (allowBiometricAuth) {
            keyType = keyType or KeyProperties.AUTH_BIOMETRIC_STRONG
        }
        if (allowDeviceCredentialAuth) {
            keyType = keyType or KeyProperties.AUTH_DEVICE_CREDENTIAL
        }

        builder.setUserAuthenticationParameters(timeout, keyType)
    }
}

/**
 * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
 */
@RequiresApi(Build.VERSION_CODES.M)
private object Api23Impl {
    @DoNotInline
    fun createKeyGenParameterSpecBuilder(
        keyName: String,
        keyPurpose: Int
    ): KeyGenParameterSpec.Builder = KeyGenParameterSpec.Builder(keyName, keyPurpose)

    @DoNotInline
    fun setBlockModeCBC(builder: KeyGenParameterSpec.Builder) {
        builder.setBlockModes(KeyProperties.BLOCK_MODE_CBC)
    }

    @DoNotInline
    fun setEncryptionPaddingPKCS7(builder: KeyGenParameterSpec.Builder) {
        builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
    }

    @DoNotInline
    fun setUserAuthenticationRequired(
        builder: KeyGenParameterSpec.Builder,
        userAuthenticationRequired: Boolean
    ) {
        builder.setUserAuthenticationRequired(userAuthenticationRequired)
    }

    @Suppress("DEPRECATION")
    @DoNotInline
    fun setUserAuthenticationValidityDurationSeconds(
        builder: KeyGenParameterSpec.Builder,
        userAuthenticationValidityDurationSeconds: Int
    ) {
        builder.setUserAuthenticationValidityDurationSeconds(
            userAuthenticationValidityDurationSeconds
        )
    }

    @DoNotInline
    fun buildKeyGenParameterSpec(builder: KeyGenParameterSpec.Builder): KeyGenParameterSpec {
        return builder.build()
    }
}