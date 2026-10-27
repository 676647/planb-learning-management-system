package fr.skyframe.lms.storage;

import com.mongodb.MongoException;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.IOException;
import java.io.InputStream;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Keeps files in MongoDB's GridFS.
 *
 * GridFS is MongoDB's built-in way to store files bigger than a single document (16 MB):
 * it splits each file into chunks (collection "fs.chunks") and keeps one metadata document
 * per file (collection "fs.files"). We use our storage key as the GridFS file name.
 */
public class GridFsFileStorage implements FileStorage {

    private final GridFsTemplate gridFs;

    public GridFsFileStorage(GridFsTemplate gridFs) {
        this.gridFs = gridFs;
    }

    @Override
    public void put(String key, InputStream data, long size, String contentType) {
        try {
            gridFs.store(data, key, contentType);
        } catch (MongoException | DataAccessException e) {
            throw new StorageException("Could not store file " + key, e);
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            GridFSFile file = gridFs.findOne(byKey(key));
            if (file == null) {
                throw new StorageException("No such file " + key, null);
            }
            return gridFs.getResource(file).getInputStream();
        } catch (MongoException | DataAccessException | IOException e) {
            throw new StorageException("Could not read file " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            gridFs.delete(byKey(key));
        } catch (MongoException | DataAccessException e) {
            throw new StorageException("Could not delete file " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return gridFs.findOne(byKey(key)) != null;
        } catch (MongoException | DataAccessException e) {
            throw new StorageException("Could not check file " + key, e);
        }
    }

    private static Query byKey(String key) {
        return Query.query(where("filename").is(key));
    }
}
