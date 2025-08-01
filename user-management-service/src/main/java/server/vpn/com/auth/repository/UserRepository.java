package server.vpn.com.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.vpn.com.auth.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Поиск пользователя по email
     * @param email email пользователя
     * @return Optional<User>
     */
    Optional<User> findByEmail(String email);

    /**
     * Проверка существования пользователя по email
     * @param email email пользователя
     * @return boolean
     */
    boolean existsByEmail(String email);

    /**
     * Поиск активного пользователя по email
     * @param email email пользователя
     * @return Optional<User>
     */
    Optional<User> findByEmailAndIsActiveTrue(String email);

    @Modifying
    @Query("UPDATE User u SET u.tokenVersion = u.tokenVersion + 1 WHERE u.id = :userId")
    void incrementTokenVersion(@Param("userId") Long userId);
}