package br.com.firestore.example.service;

import br.com.firestore.example.architecture.FirestoreCollection;
import br.com.firestore.example.exception.BusinessException;
import br.com.firestore.example.exception.IntegrationException;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirestoreService {

    private final Firestore firestore;

    public <T> List<T> getAll(Class<T> classe) {
        try {
            String colecao = getCollectionName(classe);

            if (Objects.isNull(colecao)) {
                throw new IllegalArgumentException("A classe do objeto deve ter a anotação @FirestoreCollection com um nome de coleção válido.");
            }

            List<T> objetos = new ArrayList<>();

            CollectionReference collectionReference = firestore.collection(colecao);
            ApiFuture<QuerySnapshot> querySnapshotApiFuture = collectionReference.get();

            querySnapshotApiFuture.get().getDocuments().forEach(documentSnapshot -> {
                T objeto = documentSnapshot.toObject(classe);
                preencherId(objeto, documentSnapshot.getId());
                objetos.add(objeto);
            });

            return objetos;
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegrationException("Ocorreu um erro ao buscar os dados da coleção. " + e.getMessage());
        } catch(Exception e) {
            throw new IntegrationException("Ocorreu um erro ao buscar os dados da coleção. " + e.getMessage());
        }
    }

    public <T> T getById(String documentoId, Class<T> classe) {
        try {
            String colecao = getCollectionName(classe);

            if (Objects.isNull(colecao)) {
                throw new IllegalArgumentException("A classe do objeto deve ter a anotação @FirestoreCollection com um nome de coleção válido.");
            }

            ApiFuture<DocumentSnapshot> documentSnapshotApiFuture = firestore.collection(colecao).document(documentoId).get();
            DocumentSnapshot documentSnapshot = documentSnapshotApiFuture.get();

            if (documentSnapshot.exists()) {
                T objeto = documentSnapshot.toObject(classe);
                preencherId(objeto, documentSnapshot.getId());
                return objeto;
            } else {
                throw new BusinessException("Documento não encontrado no Firestore.");
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegrationException("Ocorreu um erro ao realizar a consulta pelo ID no Firestore. " + e.getMessage());
        } catch(BusinessException e) {
            throw e;
        } catch(Exception e) {
            log.error("Ocorreu um erro ao realizar a consulta pelo ID no Firestore. " + e.getMessage());
            throw new IntegrationException("Ocorreu um erro ao realizar a consulta pelo ID no Firestore. " + e.getMessage());
        }
    }

    public <T> List<T> getByField(Class<T> classe, String field, Object value) {
        try {
            String colecao = getCollectionName(classe);

            if (Objects.isNull(colecao)) {
                throw new IllegalArgumentException("A classe do objeto deve ter a anotação @FirestoreCollection com um nome de coleção válido.");
            }

            List<T> resultados = new ArrayList<>();

            CollectionReference collectionReference = firestore.collection(colecao);
            Query query = collectionReference.whereEqualTo(field, value);
            ApiFuture<QuerySnapshot> querySnapshotApiFuture = query.get();
            querySnapshotApiFuture.get().getDocuments().forEach(documentSnapshot -> {
                T objeto = documentSnapshot.toObject(classe);
                preencherId(objeto, documentSnapshot.getId());
                resultados.add(objeto);
            });

            return resultados;
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegrationException("Ocorreu um erro ao realizar a consulta pelo campo no Firestore. " + e.getMessage());
        } catch(Exception e) {
            log.error("Ocorreu um erro ao realizar a consulta pelo campo no Firestore. " + e.getMessage());
            throw new IntegrationException("Ocorreu um erro ao realizar a consulta pelo campo no Firestore. " + e.getMessage());
        }
    }

    public void saveOrUpdate(Object objeto) {
        try {
            Class<?> classe = objeto.getClass();
            String colecao = getCollectionName(classe);

            if (Objects.isNull(colecao)) {
                throw new IntegrationException("A classe do objeto deve ter a anotação @FirestoreCollection com um nome de coleção válido.");
            }

            Field field = getIdField(classe);
            field.setAccessible(true);
            String id = (String) field.get(objeto);

            DocumentReference documentReference;
            if (Objects.nonNull(id)) {
                documentReference = firestore.collection(colecao).document(id);
            } else {
                documentReference = firestore.collection(colecao).document();
            }

            ApiFuture<WriteResult> resultado = documentReference.set(objeto);
            resultado.get();
        } catch (Exception e) {
            throw new IntegrationException("Ocorreu um erro ao salvar/atualizar o documento. " + e.getMessage());
        }
    }

    public void delete(Object objeto) {
        try {
            Class<?> classe = objeto.getClass();
            String colecao = getCollectionName(classe);

            if (colecao == null) {
                throw new IntegrationException("A classe do objeto deve ter a anotação @FirestoreCollection com um nome de coleção válido.");
            }

            Field idField = getIdField(classe);
            idField.setAccessible(true);
            String id = (String) idField.get(objeto);

            if (id == null || id.isEmpty()) {
                throw new IntegrationException("O objeto deve ter um ID definido para ser excluído.");
            }

            DocumentReference documentReference = firestore.collection(colecao).document(id);
            ApiFuture<WriteResult> resultado = documentReference.delete();
            resultado.get();
        } catch(Exception e) {
            throw new IntegrationException("Ocorreu um erro ao deletar o documento. " + e.getMessage());
        }
    }

    public String getCollectionName(Class<?> classe) {
        FirestoreCollection annotation = classe.getAnnotation(FirestoreCollection.class);
        if (Objects.nonNull(annotation)) {
            return annotation.value();
        }

        return null;
    }

    public Field getIdField(Class<?> classe) {
        for (Field field : classe.getDeclaredFields()) {
            if (field.isAnnotationPresent(DocumentId.class)) {
                return field;
            }
        }

        throw new IntegrationException("A classe do objeto deve ter um campo anotado com @DocumentId.");
    }

    public void preencherId(Object objeto, String id) {
        try {
            for (Field field : objeto.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(DocumentId.class)) {
                    field.setAccessible(true);
                    field.set(objeto, id);
                    return;
                }
            }
        } catch (Exception e) {
            throw new IntegrationException("Não foi possível definir o ID do objeto.");
        }
    }

}
