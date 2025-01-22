package com.congratulation.TestSolar.controllers;

import com.congratulation.TestSolar.models.User;
import com.congratulation.TestSolar.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class MainController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/home")
    public String greeting(Model model) {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);

        List<User> allUsers = (List<User>) userRepository.findAll();

        List<User> upcomingBirthdays = allUsers.stream()
                .filter(user -> {
                    LocalDate birthdayThisYear = user.getDate().withYear(today.getYear());
                    return !birthdayThisYear.isBefore(today) && !birthdayThisYear.isAfter(threeDaysLater);
                })
                .collect(Collectors.toList());

        model.addAttribute("upcomingBirthdays", upcomingBirthdays);
        return "home";
    }

    @GetMapping("/add")
    public String add(Model model) {
        return "add";
    }

    @GetMapping("/remove")
    public String remove(Model model) {
        model.addAttribute("title", "Удаление");
        return "remove";
    }

    @GetMapping("/all")
    public String all(Model model) {
        Iterable<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "all";
    }

    @GetMapping("/users/photo/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null && user.getPhoto() != null) {
            try {
                Blob photoBlob = user.getPhoto();
                byte[] photoBytes = photoBlob.getBytes(1, (int) photoBlob.length());
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(photoBytes);
            } catch (SQLException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/add")
    public String addBirthday(@RequestParam("fio") String fio,
                              @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              @RequestParam(value = "photo", required = false) MultipartFile photo) {
        User user = new User();
        user.setFio(fio);
        user.setDate(date);

        if (photo != null && !photo.isEmpty()) {
            try {
                Blob photoBlob = new javax.sql.rowset.serial.SerialBlob(photo.getBytes());
                user.setPhoto(photoBlob);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }

        userRepository.save(user);
        return "redirect:/all";
    }

    @GetMapping("/all/{id}/edit")
    public String edit(@PathVariable(value = "id") long id, Model model) {
        if (!userRepository.existsById(id)) {
            return "redirect:/all";
        }

        Optional<User> user = userRepository.findById(id);
        ArrayList<User> res = new ArrayList<>();
        user.ifPresent(res::add);
        model.addAttribute("user", res);
        return "edit";
    }

    @PostMapping("/all/{id}/edit")
    public String edit(@PathVariable(value = "id") long id, @RequestParam("fio") String fio,
                       @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(value = "photo", required = false) MultipartFile photo) {
        User user = userRepository.findById(id).orElseThrow();
        user.setFio(fio);
        user.setDate(date);
        if (photo != null && !photo.isEmpty()) {
            try {
                Blob photoBlob = new javax.sql.rowset.serial.SerialBlob(photo.getBytes());
                user.setPhoto(photoBlob);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
        userRepository.save(user);
        return "redirect:/all";
    }

    @PostMapping("/all/{id}/remove")
    public String remove(@PathVariable(value = "id") long id) {
        User user = userRepository.findById(id).orElseThrow();
        userRepository.delete(user);
        return "redirect:/all";
    }

    @GetMapping("/home/{id}/edit")
    public String editHome(@PathVariable(value = "id") long id, Model model) {
        if (!userRepository.existsById(id)) {
            return "redirect:/home";
        }

        Optional<User> user = userRepository.findById(id);
        ArrayList<User> res = new ArrayList<>();
        user.ifPresent(res::add);
        model.addAttribute("user", res);
        return "edit";
    }

    @PostMapping("/home/{id}/edit")
    public String editHome(@PathVariable(value = "id") long id, @RequestParam("fio") String fio,
                           @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(value = "photo", required = false) MultipartFile photo) {
        User user = userRepository.findById(id).orElseThrow();
        user.setFio(fio);
        user.setDate(date);
        if (photo != null && !photo.isEmpty()) {
            try {
                Blob photoBlob = new javax.sql.rowset.serial.SerialBlob(photo.getBytes());
                user.setPhoto(photoBlob);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
        userRepository.save(user);
        return "redirect:/home";
    }

    @PostMapping("/home/{id}/remove")
    public String removeHome(@PathVariable(value = "id") long id) {
        User user = userRepository.findById(id).orElseThrow();
        userRepository.delete(user);
        return "redirect:/home";
    }

    @GetMapping("/home/date-search")
    public String dateSearch(@RequestParam("searchDate") String searchDate, Model model) {
        List<User> allUsers = (List<User>) userRepository.findAll();

        String[] dateParts = searchDate.split("\\.");
        if (dateParts.length != 2) {
            model.addAttribute("error", "Формат даты неверный, используйте дд.мм");
            model.addAttribute("upcomingBirthdays", allUsers); // Последний результат, чтобы не показать пустую таблицу
            return "home";
        }

        int day = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);

        List<User> searchedUsers = allUsers.stream()
                .filter(user -> {
                    LocalDate birthdayThisYear = user.getDate().withYear(LocalDate.now().getYear());
                    return birthdayThisYear.getDayOfMonth() == day && birthdayThisYear.getMonthValue() == month;
                })
                .collect(Collectors.toList());

        model.addAttribute("upcomingBirthdays", searchedUsers);
        return "home";
    }

    @GetMapping("/all/date-search")
    public String dateSearchAll(@RequestParam("searchDate") String searchDate, Model model) {
        List<User> allUsers = (List<User>) userRepository.findAll();

        String[] dateParts = searchDate.split("\\.");
        if (dateParts.length != 2) {
            model.addAttribute("error", "Формат даты неверный, используйте дд.мм");
            model.addAttribute("users", allUsers);
            return "all";
        }

        int day = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);

        List<User> searchedUsers = allUsers.stream()
                .filter(user -> user.getDate().getDayOfMonth() == day &&
                        user.getDate().getMonthValue() == month)
                .collect(Collectors.toList());

        model.addAttribute("users", searchedUsers);
        return "all";
    }
}
