package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

/**
 * The service areas an address may belong to. Sites outside both are refused at validation time.
 */
public enum CoverageArea {

  /** Metropolitan Lima, excluding the Constitutional Province of Callao. */
  LIMA_METROPOLITANA,

  /** The Constitutional Province of Callao. */
  CALLAO
}
