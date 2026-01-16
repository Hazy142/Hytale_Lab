package net.deinserver.livingorbis;

public class EntropyManager {

    private float entropy;

    public EntropyManager() {
        // Initialer Entropiewert, z.B. 0.0 (Ordnung) bis 1.0 (Chaos)
        this.entropy = 0.5f;
    }

    /**
     * Modifiziert den aktuellen Entropie-Wert.
     * @param delta Die Änderung (positiv oder negativ).
     */
    public void modifyEntropy(float delta) {
        this.entropy += delta;

        // Clamp zwischen 0.0 und 1.0
        if (this.entropy < 0.0f) {
            this.entropy = 0.0f;
        } else if (this.entropy > 1.0f) {
            this.entropy = 1.0f;
        }
    }

    /**
     * Gibt den aktuellen Entropie-Wert zurück.
     * @return float zwischen 0.0 und 1.0
     */
    public float getEntropy() {
        return this.entropy;
    }
}
