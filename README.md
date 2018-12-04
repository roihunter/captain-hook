# Captain Hook

μ-service for subscriptions from Facebook

## Development setup

### Environment

```sh
pip install pipenv
pipenv install
```
To run python commands inside the virtual environment use `pipenv shell`, which gets you into the environment,
to run a single command in the environment `pipenv run <command>` can be used.

To use the virtual environment in your IDE get its location via `pipenv --venv`.

### Configuration

Create local configuration file `hooks/settings/_settings_local.py`.

## API server

* `python -m hooks`
    * debugging
* `gunicorn --workers=4 --bind 0.0.0.0:8005 hooks.app:app`
    * production ready

## API endpoints

*  `/health`
    * check if service is alive

*  `/api/v1/{scope}/fb/hooks`
	* *scope* is one of master, staging or testing

*  `/api/v1/hubspot/hooks`
	* not scoped, as Hubspot doesn't have a staging version


## Docker

Build image from Dockerfile.

`docker build -t captain-hook .`

Run container from image. API will run on port 8005 (see `supervisord.conf`).

`docker run --name captain-hook -p 8005:8005 -d captain-hook`
