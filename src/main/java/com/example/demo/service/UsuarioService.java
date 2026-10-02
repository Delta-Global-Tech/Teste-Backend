package com.example.demo.service;

import com.example.demo.dto.usuario.CreateUsuarioDTO;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Usuario save(CreateUsuarioDTO createUsuarioDTO) {
        Usuario usuario = new Usuario(createUsuarioDTO);
        return usuarioRepository.save(usuario);
    }
}
