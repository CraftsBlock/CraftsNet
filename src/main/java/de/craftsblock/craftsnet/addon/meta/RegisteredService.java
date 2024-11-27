package de.craftsblock.craftsnet.addon.meta;

/**
 * Represents a registered service, encapsulating the service provider interface (SPI)
 * and the provider information in a concise record.
 *
 * @param spi      The name of the service provider interface (SPI).
 * @param provider Information about the service provider.
 */
public record RegisteredService(String spi, String provider) {
}
