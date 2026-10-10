package com.workouttracker.service;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.dto.ProgramDto;
import com.workouttracker.exception.ApiException;
import com.workouttracker.model.Exercise;
import com.workouttracker.model.Program;
import com.workouttracker.model.ProgramDay;
import com.workouttracker.model.ProgramExercise;
import com.workouttracker.repository.ProgramRepository;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;

    public ProgramService(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    @Transactional(readOnly = true)
    public List<ProgramDto.Summary> list() {
        return programRepository.findAllByOrderByIdAsc().stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ProgramDto.Detail get(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Program not found"));

        List<ProgramDto.Day> days = program.getDays().stream().map(this::toDay).toList();
        ProgramDto.Summary summary = toSummary(program);
        return new ProgramDto.Detail(program.getId(), program.getName(),
                program.getCategory().name(), program.getCategory().getLabel(),
                program.getDescription(), summary.trainingDays(), summary.restDays(), days);
    }

    private ProgramDto.Summary toSummary(Program program) {
        int training = 0;
        int rest = 0;
        int exercises = 0;
        for (ProgramDay day : program.getDays()) {
            if (day.isRestDay()) {
                rest++;
            } else {
                training++;
            }
            exercises += day.getExercises().size();
        }
        return new ProgramDto.Summary(program.getId(), program.getName(),
                program.getCategory().name(), program.getCategory().getLabel(),
                program.getDescription(), training, rest, exercises);
    }

    private ProgramDto.Day toDay(ProgramDay day) {
        String dayName = DayOfWeek.of(day.getDayOfWeek()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        List<ProgramDto.Item> items = day.getExercises().stream().map(this::toItem).toList();
        return new ProgramDto.Day(day.getDayOfWeek(), dayName, day.getName(), day.isRestDay(), items);
    }

    private ProgramDto.Item toItem(ProgramExercise item) {
        Exercise exercise = item.getExercise();
        return new ProgramDto.Item(exercise.getId(), exercise.getName(),
                exercise.getMuscleGroup().getLabel(), exercise.getEquipment(),
                item.getSets(), item.getReps(), item.getDurationSeconds(), item.getRestSeconds());
    }
}