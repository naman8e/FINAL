package com.weather.report.model;

import java.time.LocalDateTime;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Column;

/**
 * Base class carrying audit metadata (creator/updater and timestamps).
 */
@MappedSuperclass
public class Timestamped {
  @Column(name = "created_by")
  private String createdBy;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "modified_by")
  private String modifiedBy;

  @Column(name = "modified_at")
  private LocalDateTime modifiedAt;

  /**
   * Sets the username of the creator.
   *
   * @param createdBy creator username
   */
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  /**
   * Sets the creation timestamp.
   *
   * @param createdAt creation time
   */
  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Sets the username of the last modifier.
   *
   * @param modifiedBy modifier username
   */
  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  /**
   * Sets the last modification timestamp.
   *
   * @param modifiedAt modification time
   */
  public void setModifiedAt(LocalDateTime modifiedAt) {
    this.modifiedAt = modifiedAt;
  }

  /**
   * @return username that created the entity
   */
  public String getCreatedBy() {
    return createdBy;
  }

  /**
   * @return timestamp of creation
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * @return username that last modified the entity
   */
  public String getModifiedBy() {
    return modifiedBy;
  }

  /**
   * @return timestamp of the last modification
   */
  public LocalDateTime getModifiedAt() {
    return modifiedAt;
  }

}
