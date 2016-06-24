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


## API endpoints

*  `/health`
    * check if service is alive

*  `/api/v1/{scope}/fb/hooks`
	* scope is one of master, staging or testing
