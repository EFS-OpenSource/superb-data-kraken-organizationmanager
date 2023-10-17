## Git Commands

### Create topic branch
Check out a new branch based on the development branch according to the [branch naming conventions](./branching-guidelines.md/#naming):

  ````
  $ git checkout -b BRANCH_NAME
  ````
  If you get an error, you may need to fetch first by using
  ````
  $ git remote update && git fetch
  ````

### Commit patches
To commit your changes to your local repository
  ````
  $ git commit
  ````

### Push to the remote branch
To push to the remote branch
      ````
      $ git push origin BRANCH_NAME
      ````
