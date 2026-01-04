package com.weather.report.repositories;

import com.weather.report.persistence.PersistenceManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;

/**
 * Generic repository exposing basic CRUD operations backed by the persistence
 * layer.
 * <p>
 * Concrete repositories extend/compose this class to centralise common database
 * access
 * logic for all entities, as described in the README.
 *
 * @param <T>  entity type
 * @param <ID> identifier (primary key) type
 */
public class CRUDRepository<T, ID> {

  protected Class<T> entityClass;

  /**
   * Builds a repository for the given entity class.
   *
   * @param entityClass entity class handled by this repository
   */
  public CRUDRepository(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  /**
   * Given an entity class retrieves the name of the entity to be used in the
   * queries.
   * 
   * @return the name of the entity (to be used in queries)
   */
  protected String getEntityName() {
    Entity ea = entityClass.getAnnotation(jakarta.persistence.Entity.class);
    if (ea == null)
      throw new IllegalArgumentException("Class " + this.entityClass.getName() + " must be annotated as @Entity");
    if (ea.name().isEmpty())
      return this.entityClass.getSimpleName();
    return ea.name();
  }

  /**
   * Persists a new entity instance.
   *
   * @param entity entity to persist
   * @return persisted entity
   */
  public T create(T entity) {
    EntityManager em = PersistenceManager.getEntityManager();
    try {
      em.getTransaction().begin();
      em.persist(entity);
      em.getTransaction().commit();
      return entity;
    } finally {
      em.close();
    }
  }

  /**
   * Reads a single entity by identifier.
   *
   * @param id entity identifier (primary key)
   * @return found entity or {@code null} if absent
   */
  public T read(ID id) {
    EntityManager em = PersistenceManager.getEntityManager();
    try {
      return em.find(entityClass, id);

    } finally {
      em.close();
    }
  }

  /**
   * Reads all entities of the managed type.
   *
   * @return list of all entities
   */
  public List<T> read() {
    EntityManager em = PersistenceManager.getEntityManager();
    try {
      String jpql = "SELECT e FROM " + getEntityName() + " e";
      TypedQuery<T> query = em.createQuery(jpql, entityClass);
      return query.getResultList();
    } finally {
      em.close();
    }
  }

  /**
   * Updates an existing entity.
   *
   * @param entity entity with new state
   * @return updated entity
   */
  public T update(T entity) {
    EntityManager em = PersistenceManager.getEntityManager();
    try {
      em.getTransaction().begin();
      T updated = em.merge(entity);
      em.getTransaction().commit();
      return updated;
    } finally {
      em.close();
    }

  }

  /**
   * Deletes an entity by identifier (primary key).
   *
   * @param id entity identifier (primary key)
   * @return deleted entity
   */
  public T delete(ID id) {
    EntityManager em = PersistenceManager.getEntityManager();
    try {
      em.getTransaction().begin();
      T entity = em.find(entityClass, id);
      if (entity != null) {
        em.remove(entity);
      }
      em.getTransaction().commit();
      return entity;
    } finally {
      em.close();
    }
  }
}
