package org.itmo.secs.repositories;

import org.itmo.secs.model.entities.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, Long> {
}
