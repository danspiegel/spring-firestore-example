package br.com.firestore.example.controller;

import br.com.firestore.example.domain.Cliente;
import br.com.firestore.example.service.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ClienteController {

    @Autowired
    private FirestoreService firestoreService;


    @GetMapping("/cliente")
    public List<Cliente> getAll() {
        return firestoreService.getAll(Cliente.class);
    }

    @PostMapping("/cliente")
    public void post(@RequestBody Cliente cliente) {
        firestoreService.saveOrUpdate(cliente);
    }


    @PutMapping("/cliente/{id}")
    public void update(@PathVariable String id, @RequestBody Cliente cliente) {
        cliente.setId(id);
        firestoreService.saveOrUpdate(cliente);
    }

    @DeleteMapping("/cliente/{id}")
    public void delete(@PathVariable String id) {
        Cliente cliente = firestoreService.getById(id, Cliente.class);
        firestoreService.delete(cliente);
    }

}
