package com.workouttracker.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    public enum WeightUnit { KG, LB }
    public enum Theme { LIGHT, DARK }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "weight_unit", nullable = false, length = 2)
    private WeightUnit weightUnit = WeightUnit.KG;

    @Column(name = "weekly_goal", nullable = false)
    private int weeklyGoal = 3;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 5)
    private Theme theme = Theme.LIGHT;

    @Column(name = "default_rest_seconds", nullable = false)
    private int defaultRestSeconds = 60;

    protected UserSettings() { }

    public UserSettings(User user) { this.user = user; }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public WeightUnit getWeightUnit() { return weightUnit; }
    public void setWeightUnit(WeightUnit weightUnit) { this.weightUnit = weightUnit; }
    public int getWeeklyGoal() { return weeklyGoal; }
    public void setWeeklyGoal(int weeklyGoal) { this.weeklyGoal = weeklyGoal; }
    public Theme getTheme() { return theme; }
    public void setTheme(Theme theme) { this.theme = theme; }
    public int getDefaultRestSeconds() { return defaultRestSeconds; }
    public void setDefaultRestSeconds(int defaultRestSeconds) { this.defaultRestSeconds = defaultRestSeconds; }
}