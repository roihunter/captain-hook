# Captain Hook

μ-service for subscriptions from Facebook

## Development setup
```sh
python -m venv .env
. ./.env/bin/activate  # or for BFU .env\Scripts\activate.bat
pip install -U pip wheel
pip install --use-wheel -r requirements-dev.txt
```
Create local configuration file `hooks/settings_local.py`.

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

*  `/api/v1/proxy/{url}`
	* *url* can be any url, e.g. http://www.google.com
	
## Docker

Build image from Dockerfile.

`docker build -t captain-hook .`

Run container from image. API will run on port 8005 (see `supervisord.conf`).

`docker run --name captain-hook -p 8005:8005 -d captain-hook`
