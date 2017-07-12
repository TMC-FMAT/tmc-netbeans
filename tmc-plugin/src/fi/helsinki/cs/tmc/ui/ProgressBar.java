package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.Skill;
import fi.helsinki.cs.tmc.model.CourseDb;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.List;
import javax.swing.JProgressBar;

public class ProgressBar extends JProgressBar {

    private final CourseDb courseDb;
    private final int exerciseCount;
    private int skillCount;
    
    public static final Color UNFINISHED = new Color(0xb7c4b7);
    public static final Color FINISHED = new Color(0, 153, 51);
    public static final Color ADAPTIVE = new Color(0x9fccc3);
    private static final Color GREEN = new Color(0x00FF00);
    private static final Color YELLOW = new Color(0xFFFF00);
    private static final Color ORANGE = new Color(0xFFC800);
    private static final int WIDTH = 200;
    private static final int HEIGHT = 30;

    public ProgressBar(Exercise exercise) {
        super();
        courseDb = CourseDb.getInstance();
        int week = exercise.getWeek();
        List<Exercise> exercises = courseDb.getExercisesByWeek(week);
        this.exerciseCount = exercises.size();

        this.setStringPainted(true);
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        this.setMinimum(0);
        this.setMaximum(exerciseCount);
        this.setValue(getCompletedExercises(exercises, exercise) + getCompletedSkills(week));
        this.setAlignmentX(CENTER_ALIGNMENT);
        this.setAlignmentY(CENTER_ALIGNMENT);
    }

    private int getCompletedExercises(List<Exercise> exercises, Exercise exercise) {
        int completedExercises = 0;
        for (Exercise ex : exercises) {
            if (ex.isCompleted() && !exercise.equals(ex)) {
                completedExercises++;
            }
        }
        completedExercises++;// because the submitted exercise isn't marked as completed yet.
        return completedExercises;
    }

    private int getCompletedSkills(int week) {
        List<Skill> skills = courseDb.getSkillsByWeek(week);
        skillCount = skills.size();
        int masteredSkills = 0;

        for (Skill skill : skills) {
            if (skill.isMastered()) {
                masteredSkills++;
            }
        }
        return masteredSkills;
    }

    @Override
    protected void paintComponent(Graphics g) {
        int x = getX();
        int y = getY();
        int progressWidth = convertValueToWidth(getModel().getValue());
        int skillWidth = convertValueToWidth(skillCount);
        int todoWidth = WIDTH - progressWidth - skillWidth;

        // Progress is displayed with green color.
        g.setColor(FINISHED);
        g.fillRect(x, y, progressWidth, HEIGHT);

        // Exercises are displayed with yellow color.
        g.setColor(UNFINISHED);
        g.fillRect(x + progressWidth, y, todoWidth, HEIGHT);

        // Skills are displayed with orange color.
        g.setColor(ADAPTIVE);
        g.fillRect(x + progressWidth + todoWidth, y, skillWidth, HEIGHT);
    }

    private int convertValueToWidth(int value) {
        return value * WIDTH / getModel().getMaximum();
    }
}