package android.security.keystore;
import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
/** android.security.keystore.KeyGenParameterSpec: parameters for AndroidKeyStore key generation (JET software keystore honours alias/size/curve). */
public final class KeyGenParameterSpec implements AlgorithmParameterSpec {
    private final Builder b;
    private KeyGenParameterSpec(Builder b) { this.b = b; }
    public String getKeystoreAlias() { return b.alias; }
    public int getPurposes() { return b.purposes; }
    public int getKeySize() { return b.keySize; }
    public AlgorithmParameterSpec getAlgorithmParameterSpec() { return b.spec; }
    public String[] getBlockModes() { return b.blockModes.clone(); }
    public String[] getEncryptionPaddings() { return b.encPaddings.clone(); }
    public String[] getSignaturePaddings() { return b.sigPaddings.clone(); }
    public String[] getDigests() { return b.digests.clone(); }
    public boolean isDigestsSpecified() { return b.digests.length > 0; }
    public boolean isUserAuthenticationRequired() { return b.userAuth; }
    public int getUserAuthenticationValidityDurationSeconds() { return b.authValidity; }
    public int getUserAuthenticationType() { return b.authType; }
    public boolean isRandomizedEncryptionRequired() { return b.randomized; }
    public boolean isInvalidatedByBiometricEnrollment() { return b.invalidatedByBio; }
    public boolean isStrongBoxBacked() { return b.strongBox; }
    public boolean isUnlockedDeviceRequired() { return b.unlockedRequired; }
    public X500Principal getCertificateSubject() { return b.subject; }
    public BigInteger getCertificateSerialNumber() { return b.serial; }
    public Date getCertificateNotBefore() { return b.notBefore; }
    public Date getCertificateNotAfter() { return b.notAfter; }
    public Date getKeyValidityStart() { return b.validStart; }
    public byte[] getAttestationChallenge() { return b.challenge; }
    public static final class Builder {
        String alias; int purposes, keySize = -1, authValidity = 0, authType = KeyProperties.AUTH_BIOMETRIC_STRONG; AlgorithmParameterSpec spec;
        String[] blockModes = {}, encPaddings = {}, sigPaddings = {}, digests = {};
        boolean userAuth, randomized = true, invalidatedByBio = true, strongBox, unlockedRequired;
        X500Principal subject; BigInteger serial; Date notBefore, notAfter, validStart; byte[] challenge;
        public Builder(String keystoreAlias, int purposes) { if (keystoreAlias == null || keystoreAlias.isEmpty()) throw new IllegalArgumentException("keystoreAlias must not be empty"); alias = keystoreAlias; this.purposes = purposes; }
        public Builder(KeyGenParameterSpec s) { this(s.b.alias, s.b.purposes); }
        public Builder setKeySize(int v) { keySize = v; return this; }
        public Builder setAlgorithmParameterSpec(AlgorithmParameterSpec v) { spec = v; return this; }
        public Builder setBlockModes(String... v) { blockModes = v == null ? new String[0] : v.clone(); return this; }
        public Builder setEncryptionPaddings(String... v) { encPaddings = v == null ? new String[0] : v.clone(); return this; }
        public Builder setSignaturePaddings(String... v) { sigPaddings = v == null ? new String[0] : v.clone(); return this; }
        public Builder setDigests(String... v) { digests = v == null ? new String[0] : v.clone(); return this; }
        public Builder setMgf1Digests(String... v) { return this; }
        public Builder setUserAuthenticationRequired(boolean v) { userAuth = v; return this; }
        public Builder setUserAuthenticationValidityDurationSeconds(int v) { authValidity = v; return this; }
        public Builder setUserAuthenticationParameters(int timeout, int type) { authValidity = timeout; authType = type; return this; }
        public Builder setUserAuthenticationValidWhileOnBody(boolean v) { return this; }
        public Builder setUserConfirmationRequired(boolean v) { return this; }
        public Builder setUserPresenceRequired(boolean v) { return this; }
        public Builder setRandomizedEncryptionRequired(boolean v) { randomized = v; return this; }
        public Builder setInvalidatedByBiometricEnrollment(boolean v) { invalidatedByBio = v; return this; }
        public Builder setIsStrongBoxBacked(boolean v) { strongBox = v; return this; }
        public Builder setUnlockedDeviceRequired(boolean v) { unlockedRequired = v; return this; }
        public Builder setCertificateSubject(X500Principal v) { subject = v; return this; }
        public Builder setCertificateSerialNumber(BigInteger v) { serial = v; return this; }
        public Builder setCertificateNotBefore(Date v) { notBefore = v; return this; }
        public Builder setCertificateNotAfter(Date v) { notAfter = v; return this; }
        public Builder setKeyValidityStart(Date v) { validStart = v; return this; }
        public Builder setKeyValidityEnd(Date v) { return this; }
        public Builder setKeyValidityForOriginationEnd(Date v) { return this; }
        public Builder setKeyValidityForConsumptionEnd(Date v) { return this; }
        public Builder setAttestationChallenge(byte[] v) { challenge = v; return this; }
        public Builder setDevicePropertiesAttestationIncluded(boolean v) { return this; }
        public Builder setMaxUsageCount(int v) { return this; }
        public KeyGenParameterSpec build() { return new KeyGenParameterSpec(this); }
    }
}
