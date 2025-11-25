package com.example.short_link.repository;

import com.example.short_link.dto.response.DailyClickResponse;
import com.example.short_link.dto.response.TopLinkResponse;
import com.example.short_link.entity.Link;
import com.example.short_link.entity.LinkClickLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LinkClickLogRepository extends JpaRepository<LinkClickLog, Long> {
    @Query("SELECT COUNT(c) " +
            "FROM LinkClickLog c" +
            " WHERE FUNCTION('DATE', c.clicked_at) = CURRENT_DATE")
    Long countTodayClicks();

    // số lượng click trong ngày -- có khoảng từ ngày bắt đầu tới ngày kết thúc
    @Query("""
            SELECT FUNCTION('DATE', c.clicked_at), COUNT(c)
            FROM LinkClickLog c
            WHERE c.clicked_at >= :start AND c.clicked_at <= :end
            GROUP BY FUNCTION('DATE', c.clicked_at)
            ORDER BY FUNCTION('DATE', c.clicked_at)
            """)
    List<Object[]> countClicksByDateRaw(Instant start, Instant end);

    // tìm top link trong khoảng ngày bắt đầu tới ngày kết thúc
    @Query("""
            SELECT l.shortCode, l.originalUrl, COUNT(c)
            FROM LinkClickLog c JOIN c.link l
            WHERE c.clicked_at >= :start AND c.clicked_at <= :end
            GROUP BY l.id, l.shortCode, l.originalUrl
            ORDER BY COUNT(c) DESC
            """)
    List<Object[]> findTopLinksWithRange(Instant start, Instant end);

    // tìm top link all time
    @Query("""
            SELECT l.shortCode, l.originalUrl, COUNT(c)
            FROM LinkClickLog c JOIN c.link l
            GROUP BY l.id, l.shortCode, l.originalUrl
            ORDER BY COUNT(c) DESC
            """)
    List<Object[]> findTopLinksAllTime();

    // đếm số lượng click theo khoảng ngày
    @Query("SELECT COUNT(c) " +
            "FROM LinkClickLog c " +
            "WHERE c.link = :link AND c.clicked_at >= :start AND c.clicked_at <= :end")
    Long countByLinkAndRange(@Param("link") Link link, Instant start, Instant end);

    // đếm số lượng click all time
    @Query("SELECT COUNT(c) FROM LinkClickLog c WHERE c.link = :link")
    Long countByLinkAllTime(@Param("link") Link link);

    // đếm số IP đã click theo khoảng ngày
    @Query("SELECT COUNT(DISTINCT c.ip) " +
            "FROM LinkClickLog c " +
            "WHERE c.link = :link AND c.clicked_at >= :start AND c.clicked_at <= :end")
    Long countDistinctIpWithRange(@Param("link") Link link, Instant start, Instant end);

    // đếm số IP đã click all time
    @Query("SELECT COUNT(DISTINCT c.ip) FROM LinkClickLog c WHERE c.link = :link")
    Long countDistinctIpAllTime(@Param("link") Link link);

    // đếm số lượng click theo  link cụ thể khoảng ngày
    @Query("""
            SELECT FUNCTION('DATE', c.clicked_at), COUNT(c)
            FROM LinkClickLog c
            WHERE c.link = :link
              AND c.clicked_at >= :start AND c.clicked_at <= :end
            GROUP BY FUNCTION('DATE', c.clicked_at)
            ORDER BY FUNCTION('DATE', c.clicked_at)
            """)
    List<Object[]> countDailyByLinkWithRange(@Param("link") Link link, Instant start, Instant end);

    // đếm số lượng click theo  link cụ thể all time
    @Query("""
            SELECT FUNCTION('DATE', c.clicked_at), COUNT(c)
            FROM LinkClickLog c
            WHERE c.link = :link
            GROUP BY FUNCTION('DATE', c.clicked_at)
            ORDER BY FUNCTION('DATE', c.clicked_at)
            """)
    List<Object[]> countDailyByLinkAllTime(@Param("link") Link link);


}
