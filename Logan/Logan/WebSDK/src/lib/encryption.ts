// @ts-ignore
import JSEncrypt from './js-encrypt.js';
const AES = require('crypto-js/aes');
const ENC_UTF8 = require('crypto-js/enc-utf8');
const ENC_BASE64 = require('crypto-js/enc-base64');
const CTR_MODE = require('crypto-js/mode-ctr');
const PAD_NOPADDING = require('crypto-js/pad-nopadding');
export interface RSACipherOb {
    cipherText: string;
    iv: string;
    secretKey: string;
}
function generateRandomBytes (byteLength: number): string {
    let result = '';
    while (result.length < byteLength) {
        result += Math.random()
            .toString(36)
            .substr(2, 1);
    }
    return result;
}

function aesEncrypt (
    plaintext: string,
    keyString: string,
    ivString: string,
    mode: any,
    padding: any
): string {
    const key = ENC_UTF8.parse(keyString);
    const iv = ENC_UTF8.parse(ivString);
    const cipherResult = AES.encrypt(plaintext, key, {
        mode: mode,
        padding: padding,
        iv: iv
    });
    const ciphertext = cipherResult.ciphertext;
    const ciphertextBase64 = ciphertext.toString(ENC_BASE64);
    return ciphertextBase64;
}

function rsaEncrypt (plaintext: string, publicKey: string): string {
    const en = new JSEncrypt();
    en.setPublicKey(publicKey);
    const cipher = en.encrypt(plaintext);
    return cipher;
}

/**
 * AES-128-CTR
 */
function ctrEncrypt (plaintext: string, keyString: string, ivString: string): string {
    return aesEncrypt(plaintext, keyString, ivString, CTR_MODE, PAD_NOPADDING);
}

export function encryptByRSA (
    plaintext: string,
    publicKey: string
): RSACipherOb {
    const iv = generateRandomBytes(16);
    const aesKey = generateRandomBytes(16);
    const cipherText = ctrEncrypt(plaintext, aesKey, iv);
    const secretKey = rsaEncrypt(aesKey, publicKey);
    return {
        cipherText,
        iv,
        secretKey
    };
}
