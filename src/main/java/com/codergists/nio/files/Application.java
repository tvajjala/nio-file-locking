package com.codergists.nio.files;

import com.codergists.nio.files.locks.ISessionFileLock;
import com.codergists.nio.files.model.Session;
import lombok.extern.slf4j.Slf4j;

import static com.codergists.nio.files.util.FileLockUtil.getSessionFileAbsolutePath;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Paths.get;

@Slf4j
public class Application {

  public static void main(String[] args) throws Exception {

    //create
    try (ISessionFileLock sessionFileLock = ISessionFileLock.Builder.createNewFileLock()) {
      Session session = new Session();
      session.setId("1");
      session.setName("FileLockingTest");
      sessionFileLock.write(session);
    }
    //read, write
    try (ISessionFileLock sessionFileLock = ISessionFileLock.Builder.openReadWriteLock()) {
      Session session = sessionFileLock.read();
      session.setId("2");
      sessionFileLock.write(session);
    }

    //readOnly
    try (ISessionFileLock sessionFileLock = ISessionFileLock.Builder.openReadOnlyLock(get(getSessionFileAbsolutePath()))) {
      Session session = sessionFileLock.read();
      log.info("session {}", session);
    }

    deleteIfExists(get(getSessionFileAbsolutePath()));
  }

}
