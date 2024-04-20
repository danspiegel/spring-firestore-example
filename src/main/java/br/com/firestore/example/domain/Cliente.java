package br.com.firestore.example.domain;

import br.com.firestore.example.architecture.FirestoreCollection;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FirestoreCollection("clientes")
public class Cliente {

    @DocumentId
    private String id;

    private String nome;

    private Integer idade;

}
