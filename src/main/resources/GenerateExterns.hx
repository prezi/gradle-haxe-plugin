package prezi.macros;

import haxe.macro.Context;
import haxe.macro.Type;

using Lambda;

class GenerateExterns
{
#if macro
	static function getPackage(type:Type):Array<String>
	{
		switch type {
			case TInst(classRef, typeParams):
				return classRef.get().pack;
			case TType(defRef, typeParams):
				return defRef.get().pack;
			case TEnum(enumRef, typeParams):
				return enumRef.get().pack;
			default:
				throw "Cannot get package of: " + type;
		}
	}

	static function getTypeName(type:Type):String
	{
		switch type {
			case TInst(classRef, typeParams):
				return classRef.get().name;
			case TType(defRef, typeParams):
				return defRef.get().name;
			case TEnum(enumRef, typeParams):
				return enumRef.get().name;
			default:
				throw "Cannot get name of: " + type;
		}
	}

	static function mkdir(dir:String)
	{
		if (!sys.FileSystem.exists(dir))
		{
			sys.FileSystem.createDirectory(dir);
		}
	}

	static function mkdirP(dirs:Array<String>):String
	{
		var base = "externs";
		mkdir(base);
		for (piece in dirs)
		{
			base = base + "/" + piece;
			mkdir(base);
		}
		return base;
	}

	static function makeQualifiedName(pack:Array<String>, name:String):String
	{
		if (pack.length == 0)
		{
			return name;
		}
		else
		{
			return pack.join(".") + "." + name;
		}
	}

	static function getTypeQualifiedName(type:Type, assertIsExported:Bool=true):String
	{
		var name;
		switch type {
			case TInst(classRef, typeParams):
				name = classToString(classRef.get(), typeParams);
			case TType(defRef, typeParams):
				name = classToString(defRef.get(), typeParams);
			case TEnum(enumRef, typeParams):
				name = classToString(enumRef.get(), typeParams);
			case TAbstract(absRef, typeParams):
				name = classToString(absRef.get(), typeParams);
			case TFun(args, ret):
				var l = args.map(function(a) {
						return (if (a.opt) '?' else '') + getTypeQualifiedName(a.t, assertIsExported);
					});
				l.push(getTypeQualifiedName(ret, assertIsExported));
				name = l.join('->');
			case TDynamic(t):
				if (t != null)
				{
					throw "I don't understand dynamic with inner type: " + type;
				}
				name = "Dynamic";
			case TAnonymous(typeFields):
				var fields = typeFields.get().fields.map(function(f) { 
						return f.name + ":" + getTypeQualifiedName(f.type, assertIsExported);
					});
				name = "{ " + fields.join(', ') + " }";
			default:
				throw "Cannot get qualified name of: " + type;
		}

		if (assertIsExported && !typeIsExported(type))
		{
			throw "Error: an exported type depends on a non-exported type: " + name;
		}
		return name;
	}

	static function classToString(klass:{pack:Array<String>, name:String}, typeParams:Array<Type>)
	{
		var className = makeQualifiedName(klass.pack, klass.name);
		var types = typeParams.map(function (x) { return getTypeQualifiedName(x); });

		if (types.length == 0)
		{
			return className;
		}
		else
		{
			return className + "<" + types.join(", ") + ">";
		}
	}

	static function makeMethodSignature(field:ClassField):String
	{
		var name = field.name;
		switch field.type {
			case TFun(params, t):
				var returnType = getTypeQualifiedName(t);
				var args = params.map(function(p) {
						return (if (p.opt) '?' else '') + p.name + ":" + getTypeQualifiedName(p.t);
					}).join(', ');
				return '\tpublic function $name($args):$returnType;';
			default:
				throw "Cannot make method signature from type: " + field.type;
		}
	}

	static function isPublicMethod(field:ClassField):Bool
	{
		return field.isPublic && switch field.kind {
				case FVar(_, _): false;
				case FMethod(_): true;
			};
	}

	static function isPublicVar(field:ClassField):Bool
	{
		return field.isPublic && switch field.kind {
				case FVar(_, _): true;
				case FMethod(_): false;
			};
	}

	static function makeExternTypeDef(tdef:DefType):String
	{
		var lines = [];
		var pack = tdef.pack.join(".");
		var name = tdef.name;
		var type = getTypeQualifiedName(tdef.type);

		lines.push('package $pack;');
		lines.push("");
		lines.push('extern typedef $name = $type;');
		return lines.join('\n');
	}

	static function makeExternEnum(_enum:EnumType):String
	{
		var lines = [];
		var pack = _enum.pack.join(".");

		lines.push('package $pack;');
		lines.push("");
		lines.push('extern enum ' + _enum.name);
		lines.push('{');

		for (field in _enum.constructs)
		{
			switch field.type {
				case TEnum(_, _):
					lines.push("\t" + field.name + ";");
				case TFun(params, t):
					var returnType = getTypeQualifiedName(t);
					var args = params.map(function(p) {
							return (if (p.opt) '?' else '') + p.name + ":" + getTypeQualifiedName(p.t);
						});
					lines.push("\t" + field.name + "(" + args.join(", ") + ");");
				default:
					throw "Unknown type for enum member: " + field.type;
			}
		}

		lines.push('}');
		return lines.join('\n');
	}

	static function classToType(klass:{t:Ref<ClassType>, params:Array<Type>}):Type
	{
		return TInst(klass.t, klass.params);
	}

	static function makeExternClass(klass:ClassType):String
	{
		var lines = [];

		var pack = klass.pack.join(".");
		var className = klass.name;
		var supers = klass.interfaces.map(function(x) { return "implements " + getTypeQualifiedName(classToType(x)); });
		if (klass.superClass != null)
		{
			throw "Super class is " + klass.superClass;
			supers.push("extends " + getTypeQualifiedName(classToType(klass.superClass)));
		}
		var classOrInterface = if (klass.isInterface) 'interface' else 'class';

		if (pack.length > 0)
		{
			lines.push('package $pack;');
			lines.push("");
		}
		lines.push('extern $classOrInterface $className' + " " + supers.join(", "));
		lines.push("{");

		// var varSignatures = klass.fields.get().filter(isPublicVar).map(makeVarSignature);
		// varSignatures.map(lines.push);

		var methodSignatures = klass.fields.get().filter(isPublicMethod).map(makeMethodSignature);
		methodSignatures.map(lines.push);

		lines.push("}");
		lines.push("");
		return lines.join('\n');
	}

	static function makeExternFileContent(type:Type):String
	{
		switch type {
			case TInst(classRef, typeParams):
				return makeExternClass(classRef.get());
			case TEnum(enumRef, typeParams):
				return makeExternEnum(enumRef.get());
			case TType(defRef, typeParams):
				return makeExternTypeDef(defRef.get());
			default:
				throw "Cannot make extern for: " + type;
		}
	}

	static function typeIsExported(type:Type):Bool
	{
		if (typeHasExportAnnotation(type))
		{
			return true;
		}

		return switch getTypeQualifiedName(type, false) {
			case "Int": true;
			case "Bool": true;
			default: false;
		}
	}

	static function typeHasExportAnnotation(type:Type):Bool
	{
		var meta = switch type {
			case TInst(classRef, typeParams): classRef.get().meta;
			case TType(defRef, typeParams): defRef.get().meta;
			case TEnum(enumRef, typeParams): enumRef.get().meta;
			case TAbstract(absRef, typeParams): absRef.get().meta;
			default:
				throw ("Cannot get metadata for: " + type);
				null;
		}

		return meta.has(":exportExtern");
	}

	static function generateExterns(types:Array<Type>)
	{
		var exportedTypes = types.filter(typeHasExportAnnotation);
		exportedTypes.map(function(type) {
				var folder = mkdirP(getPackage(type));
				var file = sys.io.File.write(folder + '/' + getTypeName(type) + '.hx');
				file.writeString(makeExternFileContent(type));
				file.close();
			});
	}

	public static function generate():Void
	{
		Context.onGenerate(generateExterns);
	}
#end
}
